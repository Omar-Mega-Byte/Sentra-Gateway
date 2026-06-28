package com.omar.sentra.order;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.omar.sentra.order.common.request.TrustedHeaders;
import com.omar.sentra.order.order.OrderRepository;
import com.omar.sentra.order.order.OrderSeedData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderServiceApplicationTest {
    private static final String REQUEST_ID = "8e3a95b8-6674-423e-83e6-0df84c2d66d0";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository repository;

    @BeforeEach
    void resetRepository() {
        repository.reset();
    }

    @Test
    void listIsOwnerScopedOrderedPaginatedAndRedacted() throws Exception {
        mockMvc.perform(readHeaders(get("/internal/v1/orders?page=0&size=20"), "orders-list"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", REQUEST_ID))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].id").value(OrderSeedData.OWNED_CREATED_ID.toString()))
                .andExpect(jsonPath("$.items[1].id").value(OrderSeedData.OWNED_COMPLETED_ID.toString()))
                .andExpect(jsonPath("$.items[0].ownerSubject").doesNotExist())
                .andExpect(jsonPath("$.items[0].tenantId").doesNotExist())
                .andExpect(jsonPath("$.items[0].id").value(not(OrderSeedData.FOREIGN_SUBJECT_ID.toString())));

        mockMvc.perform(readHeaders(
                        get("/internal/v1/orders?status=COMPLETED&page=0&size=1"),
                        "orders-list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].id").value(OrderSeedData.OWNED_COMPLETED_ID.toString()));
    }

    @Test
    void listRejectsUnknownDuplicateAndOutOfRangeQueryParameters() throws Exception {
        for (String query : new String[] {
            "?sort=id",
            "?page=-1",
            "?size=101",
            "?status=created",
            "?page=0&page=1"
        }) {
            mockMvc.perform(readHeaders(get("/internal/v1/orders" + query), "orders-list"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("ORD_REQUEST_INVALID"));
        }
    }

    @Test
    void singleReadHidesUnknownForeignSubjectAndForeignTenant() throws Exception {
        mockMvc.perform(readHeaders(
                        get("/internal/v1/orders/{id}", OrderSeedData.OWNED_COMPLETED_ID),
                        "orders-get"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(OrderSeedData.OWNED_COMPLETED_ID.toString()))
                .andExpect(jsonPath("$.ownerSubject").doesNotExist())
                .andExpect(jsonPath("$.tenantId").doesNotExist());

        for (String id : new String[] {
            OrderSeedData.FOREIGN_SUBJECT_ID.toString(),
            OrderSeedData.FOREIGN_TENANT_ID.toString(),
            "99999999-9999-4999-8999-999999999999"
        }) {
            mockMvc.perform(readHeaders(get("/internal/v1/orders/{id}", id), "orders-get"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ORD_ORDER_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("The requested order was not found."));
        }

        mockMvc.perform(readHeaders(get("/internal/v1/orders/not-a-uuid"), "orders-get"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ORD_REQUEST_INVALID"));
    }

    @Test
    void trustedContextDenialsHaveExactCodes() throws Exception {
        mockMvc.perform(get("/internal/v1/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ORD_TRUSTED_CONTEXT_REQUIRED"))
                .andExpect(header().exists("X-Request-Id"));

        mockMvc.perform(baseHeaders(get("/internal/v1/orders"), "API_CLIENT", "orders:read", null, "orders-list"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ORD_ACTOR_NOT_ALLOWED"));

        mockMvc.perform(baseHeaders(get("/internal/v1/orders"), "USER", "profile:read", null, "orders-list"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ORD_SCOPE_REQUIRED"));

        mockMvc.perform(baseHeaders(get("/internal/v1/orders"), "USER", "orders:read", null, "orders-get"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ORD_ROUTE_NOT_ALLOWED"));

        mockMvc.perform(readHeaders(get("/internal/v1/orders"), "orders-list")
                        .header(TrustedHeaders.SUBJECT, OrderSeedData.DEMO_SUBJECT, "other"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ORD_TRUSTED_CONTEXT_REQUIRED"));

        mockMvc.perform(readHeaders(get("/internal/v1/orders"), "orders-list")
                        .header(TrustedHeaders.CLIENT_ID, "api-client"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ORD_ACTOR_NOT_ALLOWED"));

        mockMvc.perform(readHeaders(get("/internal/v1/orders"), "orders-list")
                        .with(remoteAddress("192.0.2.10")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ORD_TRUSTED_CONTEXT_REQUIRED"));
    }

    @Test
    void createSetsServerFieldsAndSupportsUnkeyedAndKeyedRequests() throws Exception {
        long initial = repository.count();
        mockMvc.perform(writeHeaders(post("/internal/v1/orders"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"sku":"  BOOK-JAVA-25  ","quantity":1}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/internal/v1/orders/")))
                .andExpect(header().doesNotExist("Idempotency-Replayed"))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.items[0].sku").value("BOOK-JAVA-25"))
                .andExpect(jsonPath("$.ownerSubject").doesNotExist())
                .andExpect(jsonPath("$.tenantId").doesNotExist())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
        org.assertj.core.api.Assertions.assertThat(repository.count()).isEqualTo(initial + 1);
    }

    @Test
    void idempotencyReplaysAndConflictsWithoutSecondCommit() throws Exception {
        String body = "{\"items\":[{\"sku\":\"BOOK-JAVA-25\",\"quantity\":1}]}";
        String first = mockMvc.perform(writeHeaders(post("/internal/v1/orders"))
                        .header("Idempotency-Key", "integration-order-key-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Replayed", "false"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long afterFirst = repository.count();

        String replay = mockMvc.perform(baseHeaders(
                        post("/internal/v1/orders"),
                        "second-request-id",
                        OrderSeedData.DEMO_SUBJECT,
                        "USER",
                        "orders:write",
                        null,
                        "orders-create")
                        .header("Idempotency-Key", "integration-order-key-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"quantity\":1,\"sku\":\"BOOK-JAVA-25\"}]}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andExpect(header().string("X-Request-Id", "second-request-id"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        org.assertj.core.api.Assertions.assertThat(replay).isEqualTo(first);
        org.assertj.core.api.Assertions.assertThat(repository.count()).isEqualTo(afterFirst);

        mockMvc.perform(writeHeaders(post("/internal/v1/orders"))
                        .header("Idempotency-Key", "integration-order-key-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"sku\":\"BOOK-JAVA-25\",\"quantity\":2}]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORD_IDEMPOTENCY_CONFLICT"));
        org.assertj.core.api.Assertions.assertThat(repository.count()).isEqualTo(afterFirst);
    }

    @Test
    void createRejectsInvalidStrictMalformedUnsupportedAndOversizedInput() throws Exception {
        for (String body : new String[] {
            "{\"items\":[]}",
            "{\"items\":[{\"sku\":\"DUPLICATE\",\"quantity\":1},{\"sku\":\"DUPLICATE\",\"quantity\":2}]}",
            "{\"items\":[{\"sku\":\"FIRST-SKU\",\"sku\":\"SECOND-SKU\",\"quantity\":1}]}",
            "{\"items\":[{\"sku\":\"BOOK-JAVA-25\",\"quantity\":0}]}",
            "{\"ownerSubject\":\"attacker\",\"items\":[{\"sku\":\"BOOK-JAVA-25\",\"quantity\":1}]}",
            "{\"items\":[{\"sku\":\"BOOK-JAVA-25\",\"quantity\":"
        }) {
            mockMvc.perform(writeHeaders(post("/internal/v1/orders"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("ORD_REQUEST_INVALID"));
        }

        mockMvc.perform(writeHeaders(post("/internal/v1/orders"))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("{\"items\":[]}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("ORD_MEDIA_TYPE_UNSUPPORTED"));

        mockMvc.perform(writeHeaders(post("/internal/v1/orders"))
                        .header("Idempotency-Key", "contains space")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"sku\":\"BOOK-JAVA-25\",\"quantity\":1}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ORD_IDEMPOTENCY_KEY_INVALID"));

        String oversized = "{\"items\":[{\"sku\":\"" + "X".repeat(33000) + "\",\"quantity\":1}]}";
        mockMvc.perform(writeHeaders(post("/internal/v1/orders"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(oversized))
                .andExpect(status().isContentTooLarge())
                .andExpect(jsonPath("$.code").value("ORD_BODY_TOO_LARGE"));
    }

    @Test
    void administratorRouteRequiresRoleAndReturnsOwnerReferences() throws Exception {
        mockMvc.perform(baseHeaders(
                        get("/internal/v1/admin/orders?page=0&size=20"),
                        "USER",
                        null,
                        null,
                        "admin-orders-list"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ORD_ROLE_REQUIRED"));

        mockMvc.perform(adminHeaders(get(
                        "/internal/v1/admin/orders?tenantId=tenant-demo&subject=sentra-user-omar")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items[0].ownerSubject").value(OrderSeedData.DEMO_SUBJECT))
                .andExpect(jsonPath("$.items[0].tenantId").value(OrderSeedData.DEMO_TENANT));
    }

    @Test
    void healthMetricsSwaggerAndOpenApiMatchTheContract() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(readHeaders(get("/internal/v1/orders"), "orders-list"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("sentra_order_requests_total")))
                .andExpect(content().string(containsString("sentra_order_repository_operations_total")))
                .andExpect(content().string(not(containsString(OrderSeedData.DEMO_SUBJECT))))
                .andExpect(content().string(not(containsString(OrderSeedData.DEMO_TENANT))));

        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Sentra Order Service Internal API"))
                .andExpect(jsonPath("$.paths['/internal/v1/orders'].get").exists())
                .andExpect(jsonPath("$.paths['/internal/v1/orders/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/internal/v1/orders'].post").exists())
                .andExpect(jsonPath("$.paths['/internal/v1/admin/orders'].get").exists())
                .andExpect(jsonPath("$.paths['/internal/v1/orders'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/internal/v1/orders'].post.responses['413']").exists())
                .andExpect(jsonPath("$.paths['/internal/v1/orders'].post.responses['415']").exists())
                .andExpect(jsonPath("$.components.schemas.ApiError").exists())
                .andExpect(jsonPath("$.components.schemas.CreateOrderRequest.additionalProperties").value(false))
                .andExpect(jsonPath(
                        "$.paths['/internal/v1/orders'].post.parameters[?(@.name == 'Idempotency-Key')].in")
                        .value("header"))
                .andExpect(jsonPath(
                        "$.paths['/internal/v1/orders'].get.parameters[?(@.name == 'X-Sentra-Subject')].in")
                        .value("header"))
                .andExpect(jsonPath(
                        "$.paths['/internal/v1/admin/orders'].get.parameters[?(@.name == 'X-Sentra-Roles')].in")
                        .value("header"))
                .andExpect(jsonPath(
                        "$.paths['/internal/v1/orders'].get.responses['401'].headers['X-Request-Id']")
                        .exists())
                .andExpect(content().string(containsString("External JWT authentication")))
                .andExpect(content().string(containsString("direct client access")));
    }

    private static MockHttpServletRequestBuilder readHeaders(
            MockHttpServletRequestBuilder builder,
            String routeId) {
        return baseHeaders(builder, "USER", "orders:read", null, routeId);
    }

    private static MockHttpServletRequestBuilder writeHeaders(
            MockHttpServletRequestBuilder builder) {
        return baseHeaders(builder, "USER", "orders:write", null, "orders-create");
    }

    private static MockHttpServletRequestBuilder adminHeaders(
            MockHttpServletRequestBuilder builder) {
        return baseHeaders(
                builder,
                REQUEST_ID,
                "sentra-admin",
                "USER",
                null,
                "ORDER_ADMIN",
                "admin-orders-list");
    }

    private static MockHttpServletRequestBuilder baseHeaders(
            MockHttpServletRequestBuilder builder,
            String actor,
            String scopes,
            String roles,
            String routeId) {
        return baseHeaders(
                builder,
                REQUEST_ID,
                OrderSeedData.DEMO_SUBJECT,
                actor,
                scopes,
                roles,
                routeId);
    }

    private static MockHttpServletRequestBuilder baseHeaders(
            MockHttpServletRequestBuilder builder,
            String requestId,
            String subject,
            String actor,
            String scopes,
            String roles,
            String routeId) {
        builder.header(TrustedHeaders.REQUEST_ID, requestId)
                .header(TrustedHeaders.SUBJECT, subject)
                .header(TrustedHeaders.ACTOR_TYPE, actor)
                .header(TrustedHeaders.TENANT_ID, OrderSeedData.DEMO_TENANT)
                .header(TrustedHeaders.ROUTE_ID, routeId);
        if (scopes != null) {
            builder.header(TrustedHeaders.SCOPES, scopes);
        }
        if (roles != null) {
            builder.header(TrustedHeaders.ROLES, roles);
        }
        return builder;
    }

    private static RequestPostProcessor remoteAddress(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }
}
