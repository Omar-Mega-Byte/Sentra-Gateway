package com.sentra.notification.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sentra.notification.common.request.SentraHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Integration tests for the documented internal HTTP contract.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void listNotificationsReturnsOwnedSeedDataOnly() throws Exception {
        mvc.perform(readHeaders(get("/internal/v1/notifications?page=0&size=20")))
                .andExpect(status().isOk())
                .andExpect(header().string(SentraHeaders.RESPONSE_REQUEST_ID, "test-read-001"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items[0].id").value("70000000-0000-4000-8000-000000000002"))
                .andExpect(jsonPath("$.items[1].id").value("70000000-0000-4000-8000-000000000001"))
                .andExpect(jsonPath("$.items[0].subject").doesNotExist())
                .andExpect(jsonPath("$.items[0].tenantId").doesNotExist());
    }

    @Test
    void listNotificationsFiltersAndRejectsUnknownQuery() throws Exception {
        mvc.perform(readHeaders(get("/internal/v1/notifications?channel=EMAIL&status=SENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].channel").value("EMAIL"));

        mvc.perform(readHeaders(get("/internal/v1/notifications?unexpected=true")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NTF_REQUEST_INVALID"));
    }

    @Test
    void trustedContextDenialsUseDocumentedCodes() throws Exception {
        mvc.perform(get("/internal/v1/notifications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NTF_TRUSTED_CONTEXT_REQUIRED"));

        mvc.perform(baseHeaders(get("/internal/v1/notifications"), "notifications-list")
                        .header(SentraHeaders.SUBJECT, "sentra-user-omar")
                        .header(SentraHeaders.ACTOR_TYPE, "SERVICE")
                        .header(SentraHeaders.TENANT_ID, "tenant-demo")
                        .header(SentraHeaders.SCOPES, "notifications:read"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NTF_ACTOR_NOT_ALLOWED"));

        mvc.perform(baseHeaders(get("/internal/v1/notifications"), "notifications-list")
                        .header(SentraHeaders.SUBJECT, "sentra-user-omar")
                        .header(SentraHeaders.ACTOR_TYPE, "USER")
                        .header(SentraHeaders.TENANT_ID, "tenant-demo")
                        .header(SentraHeaders.SCOPES, "profile:read"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NTF_SCOPE_REQUIRED"));

        mvc.perform(baseHeaders(get("/internal/v1/notifications"), "admin-test-notification")
                        .header(SentraHeaders.SUBJECT, "sentra-user-omar")
                        .header(SentraHeaders.ACTOR_TYPE, "USER")
                        .header(SentraHeaders.TENANT_ID, "tenant-demo")
                        .header(SentraHeaders.SCOPES, "notifications:read"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NTF_ROUTE_NOT_ALLOWED"));
    }

    @Test
    void preferenceUpdateUsesOptimisticVersioningAndStrictJson() throws Exception {
        String body = "{\"emailEnabled\":true,\"smsEnabled\":false,\"pushEnabled\":true,\"webhookEnabled\":false,\"version\":2}";
        mvc.perform(writeHeaders(post("/internal/v1/preferences"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(3))
                .andExpect(jsonPath("$.updatedAt").exists());

        mvc.perform(writeHeaders(post("/internal/v1/preferences"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NTF_VERSION_CONFLICT"));

        mvc.perform(writeHeaders(post("/internal/v1/preferences"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emailEnabled\":true,\"smsEnabled\":false,\"pushEnabled\":true,\"webhookEnabled\":false,\"version\":3,\"extra\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NTF_REQUEST_INVALID"));
    }

    @Test
    void postMediaAndBodyLimitsAreEnforced() throws Exception {
        mvc.perform(writeHeaders(post("/internal/v1/preferences"))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("{}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("NTF_MEDIA_TYPE_UNSUPPORTED"));

        mvc.perform(writeHeaders(post("/internal/v1/preferences"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("x".repeat(17000)))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("NTF_BODY_TOO_LARGE"));
    }

    @Test
    void adminTestSupportsSuccessAndFaultScenarios() throws Exception {
        String success = "{\"scenario\":\"SUCCESS\",\"channel\":\"EMAIL\",\"recipientReference\":\"test-recipient\",\"message\":\"Gateway resilience smoke test\"}";
        mvc.perform(adminHeaders(post("/internal/v1/test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(success))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(true))
                .andExpect(jsonPath("$.result").value("TEST_ACCEPTED"));

        String failure = "{\"scenario\":\"FAILURE\",\"channel\":\"EMAIL\",\"recipientReference\":\"test-recipient\",\"message\":\"Gateway resilience smoke test\"}";
        mvc.perform(adminHeaders(post("/internal/v1/test"))
                        .header(SentraHeaders.TEST_STATUS, "503")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(failure))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("NTF_TEST_FAILURE"));

        String malformed = "{\"scenario\":\"MALFORMED\",\"channel\":\"EMAIL\",\"recipientReference\":\"test-recipient\",\"message\":\"Gateway resilience smoke test\"}";
        mvc.perform(adminHeaders(post("/internal/v1/test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformed))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"scenario\":\"MALFORMED\",\"accepted\":"));
    }

    @Test
    void adminRoleAndHeaderFaultDenialsUseDocumentedCodes() throws Exception {
        String success = "{\"scenario\":\"SUCCESS\",\"channel\":\"EMAIL\",\"recipientReference\":\"test-recipient\",\"message\":\"Gateway resilience smoke test\"}";
        mvc.perform(baseHeaders(post("/internal/v1/test"), "admin-test-notification")
                        .header(SentraHeaders.SUBJECT, "sentra-admin")
                        .header(SentraHeaders.ACTOR_TYPE, "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(success))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NTF_ROLE_REQUIRED"));

        mvc.perform(readHeaders(get("/internal/v1/notifications")).header(SentraHeaders.TEST_STATUS, "503"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("NTF_TEST_FAILURE"));
    }

    @Test
    void managementOpenApiAndMetricsAreAvailableInTestProfile() throws Exception {
        mvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk());

        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/internal/v1/notifications")))
                .andExpect(content().string(containsString("/internal/v1/preferences")))
                .andExpect(content().string(containsString("/internal/v1/test")))
                .andExpect(content().string(containsString("X-Sentra-Request-Id")))
                .andExpect(content().string(containsString("NTF_VERSION_CONFLICT")))
                .andExpect(content().string(containsString("gateway owns JWT validation")));

        mvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("sentra_notification_requests_total")))
                .andExpect(content().string(not(containsString("sentra-user-omar"))))
                .andExpect(content().string(not(containsString("tenant-demo"))));
    }

    private MockHttpServletRequestBuilder readHeaders(MockHttpServletRequestBuilder request) {
        return baseHeaders(request, "notifications-list")
                .header(SentraHeaders.SUBJECT, "sentra-user-omar")
                .header(SentraHeaders.ACTOR_TYPE, "USER")
                .header(SentraHeaders.TENANT_ID, "tenant-demo")
                .header(SentraHeaders.SCOPES, "notifications:read");
    }

    private MockHttpServletRequestBuilder writeHeaders(MockHttpServletRequestBuilder request) {
        return baseHeaders(request, "notification-preferences-update")
                .header(SentraHeaders.SUBJECT, "sentra-user-omar")
                .header(SentraHeaders.ACTOR_TYPE, "USER")
                .header(SentraHeaders.TENANT_ID, "tenant-demo")
                .header(SentraHeaders.SCOPES, "notifications:write");
    }

    private MockHttpServletRequestBuilder adminHeaders(MockHttpServletRequestBuilder request) {
        return baseHeaders(request, "admin-test-notification")
                .header(SentraHeaders.SUBJECT, "sentra-admin")
                .header(SentraHeaders.ACTOR_TYPE, "USER")
                .header(SentraHeaders.ROLES, "NOTIFICATION_ADMIN");
    }

    private MockHttpServletRequestBuilder baseHeaders(MockHttpServletRequestBuilder request, String routeId) {
        return request.header(SentraHeaders.REQUEST_ID, "test-read-001")
                .header(SentraHeaders.ROUTE_ID, routeId);
    }
}
