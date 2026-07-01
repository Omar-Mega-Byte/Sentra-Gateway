package com.omar.sentra.payment;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.omar.sentra.payment.common.request.TrustedHeaders;
import com.omar.sentra.payment.idempotency.IdempotencyStore;
import com.omar.sentra.payment.payment.PaymentRepository;
import com.omar.sentra.payment.payment.PaymentSeedData;
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
class PaymentServiceApplicationTest {
    private static final String REQUEST_ID = "8e3a95b8-6674-423e-83e6-0df84c2d66d0";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository repository;

    @Autowired
    private IdempotencyStore idempotencyStore;

    @BeforeEach
    void resetState() {
        repository.reset();
        idempotencyStore.reset();
    }

    @Test
    void paymentReadIsClientScopedAndRedacted() throws Exception {
        mockMvc.perform(readHeaders(get(
                        "/internal/v1/payments/{id}",
                        PaymentSeedData.ACME_CAPTURED_PAYMENT_ID)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", REQUEST_ID))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(PaymentSeedData.ACME_CAPTURED_PAYMENT_ID.toString()))
                .andExpect(jsonPath("$.merchantReference").value("acme-order-1001"))
                .andExpect(jsonPath("$.amount").value("125.50"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("CAPTURED"))
                .andExpect(jsonPath("$.clientId").doesNotExist());

        for (String id : new String[] {
            PaymentSeedData.OTHER_CAPTURED_PAYMENT_ID.toString(),
            "99999999-9999-4999-8999-999999999999"
        }) {
            mockMvc.perform(readHeaders(get("/internal/v1/payments/{id}", id)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("PAY_PAYMENT_NOT_FOUND"));
        }

        mockMvc.perform(readHeaders(get("/internal/v1/payments/not-a-uuid")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PAY_REQUEST_INVALID"));
    }

    @Test
    void trustedContextAndSignatureDenialsHaveExactCodes() throws Exception {
        mockMvc.perform(get("/internal/v1/payments/{id}", PaymentSeedData.ACME_CAPTURED_PAYMENT_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("PAY_TRUSTED_CONTEXT_REQUIRED"))
                .andExpect(header().exists("X-Request-Id"));

        mockMvc.perform(baseHeaders(
                        get("/internal/v1/payments/{id}", PaymentSeedData.ACME_CAPTURED_PAYMENT_ID),
                        "USER",
                        "payments:read",
                        "partner-payment-read"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PAY_ACTOR_NOT_ALLOWED"));

        mockMvc.perform(baseHeaders(
                        get("/internal/v1/payments/{id}", PaymentSeedData.ACME_CAPTURED_PAYMENT_ID),
                        "API_CLIENT",
                        "refunds:write",
                        "partner-payment-read"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PAY_SCOPE_REQUIRED"));

        mockMvc.perform(baseHeaders(
                        get("/internal/v1/payments/{id}", PaymentSeedData.ACME_CAPTURED_PAYMENT_ID),
                        "API_CLIENT",
                        "payments:read",
                        "partner-payment-create"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PAY_ROUTE_NOT_ALLOWED"));

        mockMvc.perform(readHeaders(get("/internal/v1/payments/{id}", PaymentSeedData.ACME_CAPTURED_PAYMENT_ID))
                        .header(TrustedHeaders.CLIENT_ID, "partner-acme", "partner-other"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("PAY_TRUSTED_CONTEXT_REQUIRED"));

        mockMvc.perform(readHeaders(get("/internal/v1/payments/{id}", PaymentSeedData.ACME_CAPTURED_PAYMENT_ID))
                        .header("Authorization", "Bearer should-not-arrive"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("PAY_TRUSTED_CONTEXT_REQUIRED"));

        mockMvc.perform(readHeaders(get("/internal/v1/payments/{id}", PaymentSeedData.ACME_CAPTURED_PAYMENT_ID))
                        .with(remoteAddress("192.0.2.10")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("PAY_TRUSTED_CONTEXT_REQUIRED"));

        mockMvc.perform(baseHeaders(
                        post("/internal/v1/payments"),
                        "API_CLIENT",
                        "payments:write",
                        "partner-payment-create")
                        .header(TrustedHeaders.SIGNATURE_VERIFIED, "false")
                        .header(TrustedHeaders.SIGNATURE_KEY_ID, "sig-key-acme-active")
                        .header(TrustedHeaders.NONCE_STATUS, "accepted")
                        .header("Idempotency-Key", "idem-missing-signature-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody("acme-order-signature-denied", "12.00")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PAY_SIGNATURE_CONTEXT_REQUIRED"));

        mockMvc.perform(baseHeaders(
                        post("/internal/v1/payments"),
                        "API_CLIENT",
                        "payments:write",
                        "partner-payment-create")
                        .header("Idempotency-Key", "idem-no-signature-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody("acme-order-no-signature", "12.00")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PAY_SIGNATURE_CONTEXT_REQUIRED"));
    }

    @Test
    void createPaymentRequiresIdempotencyAndReplaysWithoutSecondCommit() throws Exception {
        String body = paymentBody("acme-order-postman-001", "125.50");
        String first = mockMvc.perform(writeHeaders(post("/internal/v1/payments"), "payment-create-key-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/internal/v1/payments/")))
                .andExpect(header().string("Idempotency-Replayed", "false"))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.status").value("AUTHORIZED"))
                .andExpect(jsonPath("$.clientId").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long afterFirst = repository.paymentCount();

        String replay = mockMvc.perform(writeHeaders(
                        post("/internal/v1/payments"),
                        "payment-create-key-0001",
                        "second-payment-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"USD\",\"amount\":\"125.50\",\"merchantReference\":\"acme-order-postman-001\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andExpect(header().string("X-Request-Id", "second-payment-request"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        org.assertj.core.api.Assertions.assertThat(replay).isEqualTo(first);
        org.assertj.core.api.Assertions.assertThat(repository.paymentCount()).isEqualTo(afterFirst);

        mockMvc.perform(writeHeaders(post("/internal/v1/payments"), "payment-create-key-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody("acme-order-postman-001", "126.00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PAY_IDEMPOTENCY_CONFLICT"));

        mockMvc.perform(signedHeaders(post("/internal/v1/payments"), null, "partner-payment-create", "payments:write")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody("acme-order-missing-key", "10.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PAY_IDEMPOTENCY_KEY_REQUIRED"));
    }

    @Test
    void createPaymentRejectsInvalidStrictUnsupportedOversizedAndReferenceConflict() throws Exception {
        for (String body : new String[] {
            "{\"merchantReference\":\"bad ref\",\"amount\":\"12.00\",\"currency\":\"USD\"}",
            "{\"merchantReference\":\"acme-order-invalid\",\"amount\":\"12.345\",\"currency\":\"USD\"}",
            "{\"merchantReference\":\"acme-order-invalid\",\"amount\":\"12.00\",\"currency\":\"usd\"}",
            "{\"merchantReference\":\"acme-order-invalid\",\"amount\":\"12.00\",\"currency\":\"USD\",\"id\":\"server-field\"}",
            "{\"merchantReference\":\"acme-order-invalid\",\"amount\":"
        }) {
            mockMvc.perform(writeHeaders(post("/internal/v1/payments"), "invalid-" + Math.abs(body.hashCode()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("PAY_REQUEST_INVALID"));
        }

        mockMvc.perform(writeHeaders(post("/internal/v1/payments"), "reference-conflict-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody("acme-order-1001", "12.00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PAY_REFERENCE_CONFLICT"));

        mockMvc.perform(writeHeaders(post("/internal/v1/payments"), "media-type-001")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(paymentBody("acme-order-media", "12.00")))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("PAY_MEDIA_TYPE_UNSUPPORTED"));

        String oversized = "{\"merchantReference\":\"" + "X".repeat(17000) + "\",\"amount\":\"12.00\",\"currency\":\"USD\"}";
        mockMvc.perform(writeHeaders(post("/internal/v1/payments"), "oversized-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(oversized))
                .andExpect(status().isContentTooLarge())
                .andExpect(jsonPath("$.code").value("PAY_BODY_TOO_LARGE"));
    }

    @Test
    void createRefundSucceedsReplaysAndProtectsPaymentOwnershipAndState() throws Exception {
        String body = refundBody("acme-refund-postman-001", "25.00");
        String first = mockMvc.perform(refundHeaders(post("/internal/v1/refunds"), "refund-create-key-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/internal/v1/refunds/")))
                .andExpect(header().string("Idempotency-Replayed", "false"))
                .andExpect(jsonPath("$.paymentId").value(PaymentSeedData.ACME_CAPTURED_PAYMENT_ID.toString()))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.clientId").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long afterFirst = repository.refundCount();

        String replay = mockMvc.perform(refundHeaders(
                        post("/internal/v1/refunds"),
                        "refund-create-key-0001",
                        "second-refund-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        org.assertj.core.api.Assertions.assertThat(replay).isEqualTo(first);
        org.assertj.core.api.Assertions.assertThat(repository.refundCount()).isEqualTo(afterFirst);

        mockMvc.perform(refundHeaders(post("/internal/v1/refunds"), "refund-conflict-key-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refundBody("acme-refund-postman-002", "26.00")))
                .andExpect(status().isCreated());
        mockMvc.perform(refundHeaders(post("/internal/v1/refunds"), "refund-conflict-key-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refundBody("acme-refund-postman-002", "27.00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PAY_IDEMPOTENCY_CONFLICT"));

        mockMvc.perform(refundHeaders(post("/internal/v1/refunds"), "refund-foreign-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentId":"50000000-0000-4000-8000-000000000001","amount":"1.00"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PAY_PAYMENT_NOT_FOUND"));

        mockMvc.perform(refundHeaders(post("/internal/v1/refunds"), "refund-declined-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentId":"40000000-0000-4000-8000-000000000002","amount":"1.00"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PAY_REFUND_NOT_ALLOWED"));

        mockMvc.perform(refundHeaders(post("/internal/v1/refunds"), "refund-unknown-field-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentId":"40000000-0000-4000-8000-000000000001","amount":"1.00","currency":"USD"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PAY_REQUEST_INVALID"));
    }

    @Test
    void healthMetricsSwaggerAndOpenApiMatchTheContract() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(readHeaders(get("/internal/v1/payments/{id}", PaymentSeedData.ACME_CAPTURED_PAYMENT_ID)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("sentra_payment_requests_total")))
                .andExpect(content().string(containsString("sentra_payment_repository_operations_total")))
                .andExpect(content().string(not(containsString(PaymentSeedData.ACME_CLIENT))));

        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Sentra Payment Service Internal API"))
                .andExpect(jsonPath("$.paths['/internal/v1/payments/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/internal/v1/payments'].post").exists())
                .andExpect(jsonPath("$.paths['/internal/v1/refunds'].post").exists())
                .andExpect(jsonPath("$.paths['/internal/v1/payments'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/internal/v1/payments'].post.responses['413']").exists())
                .andExpect(jsonPath("$.paths['/internal/v1/payments'].post.responses['415']").exists())
                .andExpect(jsonPath("$.components.schemas.ApiError").exists())
                .andExpect(jsonPath("$.components.schemas.CreatePaymentRequest.additionalProperties").value(false))
                .andExpect(jsonPath(
                        "$.paths['/internal/v1/payments'].post.parameters[?(@.name == 'Idempotency-Key')].in")
                        .value("header"))
                .andExpect(jsonPath(
                        "$.paths['/internal/v1/payments'].post.parameters[?(@.name == 'X-Sentra-Signature-Verified')].in")
                        .value("header"))
                .andExpect(content().string(containsString("API-key validation")))
                .andExpect(content().string(containsString("Direct client access")));
    }

    private static MockHttpServletRequestBuilder readHeaders(MockHttpServletRequestBuilder builder) {
        return baseHeaders(builder, "API_CLIENT", "payments:read", "partner-payment-read");
    }

    private static MockHttpServletRequestBuilder writeHeaders(
            MockHttpServletRequestBuilder builder,
            String idempotencyKey) {
        return writeHeaders(builder, idempotencyKey, REQUEST_ID);
    }

    private static MockHttpServletRequestBuilder writeHeaders(
            MockHttpServletRequestBuilder builder,
            String idempotencyKey,
            String requestId) {
        return signedHeaders(builder, idempotencyKey, "partner-payment-create", "payments:write", requestId);
    }

    private static MockHttpServletRequestBuilder refundHeaders(
            MockHttpServletRequestBuilder builder,
            String idempotencyKey) {
        return refundHeaders(builder, idempotencyKey, REQUEST_ID);
    }

    private static MockHttpServletRequestBuilder refundHeaders(
            MockHttpServletRequestBuilder builder,
            String idempotencyKey,
            String requestId) {
        return signedHeaders(builder, idempotencyKey, "partner-refund-create", "refunds:write", requestId);
    }

    private static MockHttpServletRequestBuilder signedHeaders(
            MockHttpServletRequestBuilder builder,
            String idempotencyKey,
            String routeId,
            String scope) {
        return signedHeaders(builder, idempotencyKey, routeId, scope, REQUEST_ID);
    }

    private static MockHttpServletRequestBuilder signedHeaders(
            MockHttpServletRequestBuilder builder,
            String idempotencyKey,
            String routeId,
            String scope,
            String requestId) {
        baseHeaders(builder, requestId, "API_CLIENT", scope, routeId)
                .header(TrustedHeaders.SIGNATURE_VERIFIED, "true")
                .header(TrustedHeaders.SIGNATURE_KEY_ID, "sig-key-acme-active")
                .header(TrustedHeaders.NONCE_STATUS, "accepted");
        if (idempotencyKey != null) {
            builder.header("Idempotency-Key", idempotencyKey);
        }
        return builder;
    }

    private static MockHttpServletRequestBuilder baseHeaders(
            MockHttpServletRequestBuilder builder,
            String actor,
            String scopes,
            String routeId) {
        return baseHeaders(builder, REQUEST_ID, actor, scopes, routeId);
    }

    private static MockHttpServletRequestBuilder baseHeaders(
            MockHttpServletRequestBuilder builder,
            String requestId,
            String actor,
            String scopes,
            String routeId) {
        return builder.header(TrustedHeaders.REQUEST_ID, requestId)
                .header(TrustedHeaders.ACTOR_TYPE, actor)
                .header(TrustedHeaders.CLIENT_ID, PaymentSeedData.ACME_CLIENT)
                .header(TrustedHeaders.KEY_ID, "key-acme-active")
                .header(TrustedHeaders.SCOPES, scopes)
                .header(TrustedHeaders.ROUTE_ID, routeId);
    }

    private static RequestPostProcessor remoteAddress(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }

    private static String paymentBody(String reference, String amount) {
        return """
                {"merchantReference":"%s","amount":"%s","currency":"USD"}
                """.formatted(reference, amount);
    }

    private static String refundBody(String reference, String amount) {
        return """
                {"paymentId":"40000000-0000-4000-8000-000000000001","merchantReference":"%s","amount":"%s"}
                """.formatted(reference, amount);
    }
}
