package com.omar.sentra.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewayApplicationTest {
    @LocalServerPort
    int port;

    WebTestClient client;

    @BeforeEach
    void setUpClient() {
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(20))
                .build();
    }

    @Test
    void exposesHealthAndSwagger() {
        client.get().uri("/actuator/health/liveness").exchange().expectStatus().isOk();
        client.get().uri("/v3/api-docs").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.info.title").isEqualTo("Sentra Gateway Administration API")
                .jsonPath("$.paths['/api/v1/admin/routes']").exists()
                .jsonPath("$.paths['/api/v1/admin/api-clients']").exists()
                .jsonPath("$.paths['/api/v1/admin/rate-limits']").exists()
                .jsonPath("$.paths['/api/v1/admin/ip-rules']").exists()
                .jsonPath("$.paths['/api/v1/admin/risk-rules']").exists()
                .jsonPath("$.paths['/api/v1/admin/audit-events']").exists();
    }

    @Test
    void protectsAdminApi() {
        client.get().uri("/api/v1/admin/routes").exchange().expectStatus().isUnauthorized();

        client.get().uri("/api/v1/admin/routes")
                .headers(headers -> headers.setBasicAuth("operator", "password"))
                .exchange()
                .expectStatus().isOk();

        client.post().uri("/api/v1/admin/routes/validate")
                .headers(headers -> headers.setBasicAuth("operator", "password"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(routeBody("operator-forbidden", 0, true))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void createsReadsUpdatesAndDeletesRoute() {
        Map<String, Object> route = routeBody("orders-list", 0, true);
        client.post().uri("/api/v1/admin/routes")
                .headers(headers -> headers.setBasicAuth("admin", "password"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(route)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo("orders-list")
                .jsonPath("$.version").isEqualTo(1);

        client.get().uri("/api/v1/admin/routes/orders-list")
                .headers(headers -> headers.setBasicAuth("admin", "password"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.targetUri").isEqualTo("http://order-service:8082");

        client.put().uri("/api/v1/admin/routes/orders-list")
                .headers(headers -> headers.setBasicAuth("admin", "password"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(routeBody("orders-list", 1, false))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.enabled").isEqualTo(false)
                .jsonPath("$.version").isEqualTo(2);

        client.delete().uri("/api/v1/admin/routes/orders-list")
                .headers(headers -> headers.setBasicAuth("admin", "password"))
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void rejectsUnsafeRoutesAndStaleVersions() {
        Map<String, Object> unsafe = new java.util.LinkedHashMap<>(routeBody("unsafe-route", 0, true));
        unsafe.put("targetUri", "http://169.254.169.254/latest/meta-data");
        client.post().uri("/api/v1/admin/routes")
                .headers(headers -> headers.setBasicAuth("admin", "password"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(unsafe)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("GW_REQUEST_INVALID");

        client.post().uri("/api/v1/admin/routes")
                .headers(headers -> headers.setBasicAuth("admin", "password"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(routeBody("versioned-route", 0, true))
                .exchange()
                .expectStatus().isCreated();

        client.put().uri("/api/v1/admin/routes/versioned-route")
                .headers(headers -> headers.setBasicAuth("admin", "password"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(routeBody("versioned-route", 0, false))
                .exchange()
                .expectStatus().isEqualTo(org.springframework.http.HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.code").isEqualTo("GW_POLICY_CONFLICT");

        client.delete().uri("/api/v1/admin/routes/versioned-route")
                .headers(headers -> headers.setBasicAuth("admin", "password"))
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    @SuppressWarnings("unchecked")
    void createsClientAndIssuesOneTimeApiKey() {
        Map<String, Object> createdClient = client.post().uri("/api/v1/admin/api-clients")
                .headers(headers -> headers.setBasicAuth("admin", "password"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "postman-partner", "owner", "qa", "tenantId", "tenant-a"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();
        assertThat(createdClient).isNotNull();
        assertThat(createdClient.get("status")).isEqualTo("ACTIVE");
        String clientId = createdClient.get("id").toString();

        client.post().uri("/api/v1/admin/api-clients/" + clientId + "/keys")
                .headers(headers -> {
                    headers.setBasicAuth("admin", "password");
                    headers.set("Idempotency-Key", "postman-key-1");
                })
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "scopes", java.util.List.of("payments:write"),
                        "allowedRoutes", java.util.List.of("payment-create"),
                        "expiresAt", Instant.now().plusSeconds(3600).toString()))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.apiKey").value(key -> assertThat(key.toString()).startsWith("sgw_test_"))
                .jsonPath("$.warning").exists();

        client.get().uri("/api/v1/admin/api-clients/" + clientId + "/keys")
                .headers(headers -> headers.setBasicAuth("admin", "password"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].verifier").doesNotExist()
                .jsonPath("$[0].status").isEqualTo("ACTIVE");
    }

    @Test
    void createsUpdatesAndDeletesAllPolicyTypes() {
        exercisePolicy(
                "/api/v1/admin/rate-limits",
                "test-rate",
                Map.ofEntries(
                        Map.entry("id", "test-rate"),
                        Map.entry("subjectType", "CLIENT"),
                        Map.entry("routeId", "payment-create"),
                        Map.entry("method", "POST"),
                        Map.entry("capacity", 10),
                        Map.entry("refillTokens", 10),
                        Map.entry("refillPeriodSeconds", 60),
                        Map.entry("priority", 100),
                        Map.entry("redisOutageMode", "DENY"),
                        Map.entry("responseHeadersEnabled", true),
                        Map.entry("enabled", true),
                        Map.entry("version", 0)));

        exercisePolicy(
                "/api/v1/admin/ip-rules",
                "test-ip",
                Map.ofEntries(
                        Map.entry("id", "test-ip"),
                        Map.entry("network", "203.0.113.0/24"),
                        Map.entry("action", "BLOCK"),
                        Map.entry("routeId", "payment-create"),
                        Map.entry("priority", 100),
                        Map.entry("reason", "Integration test"),
                        Map.entry("validFrom", Instant.now().minusSeconds(60).toString()),
                        Map.entry("enabled", true),
                        Map.entry("version", 0)));

        exercisePolicy(
                "/api/v1/admin/risk-rules",
                "test-risk",
                Map.of(
                        "id", "test-risk",
                        "signal", "HEADER_COUNT",
                        "thresholdValue", 50,
                        "weight", 25,
                        "action", "OBSERVE",
                        "routeId", "payment-create",
                        "enabled", true,
                        "version", 0));
    }

    @Test
    void searchesAuditAndAdminActionsWithinBounds() {
        Instant now = Instant.now();
        client.get().uri(uriBuilder -> uriBuilder
                        .path("/api/v1/admin/audit-events")
                        .queryParam("from", now.minus(1, ChronoUnit.HOURS))
                        .queryParam("to", now.plus(1, ChronoUnit.MINUTES))
                        .queryParam("pageSize", 100)
                        .build())
                .headers(headers -> headers.setBasicAuth("admin", "password"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray();

        client.get().uri("/api/v1/admin/admin-actions?page=0&pageSize=100")
                .headers(headers -> headers.setBasicAuth("admin", "password"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray();

        client.get().uri(uriBuilder -> uriBuilder
                        .path("/api/v1/admin/audit-events")
                        .queryParam("from", now.minus(40, ChronoUnit.DAYS))
                        .queryParam("to", now)
                        .build())
                .headers(headers -> headers.setBasicAuth("admin", "password"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("GW_REQUEST_INVALID");
    }

    private void exercisePolicy(String basePath, String id, Map<String, Object> createBody) {
        client.post().uri(basePath)
                .headers(headers -> headers.setBasicAuth("admin", "password"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createBody)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(id)
                .jsonPath("$.version").isEqualTo(1);

        Map<String, Object> updateBody = new java.util.LinkedHashMap<>(createBody);
        updateBody.put("version", 1);
        updateBody.put("enabled", false);
        client.put().uri(basePath + "/" + id)
                .headers(headers -> headers.setBasicAuth("admin", "password"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.enabled").isEqualTo(false)
                .jsonPath("$.version").isEqualTo(2);

        client.delete().uri(basePath + "/" + id)
                .headers(headers -> headers.setBasicAuth("admin", "password"))
                .exchange()
                .expectStatus().isNoContent();
    }

    private Map<String, Object> routeBody(String id, long version, boolean enabled) {
        return Map.ofEntries(
                Map.entry("id", id),
                Map.entry("category", "USER"),
                Map.entry("pathPatterns", java.util.List.of("/api/v1/orders")),
                Map.entry("methods", java.util.List.of("GET")),
                Map.entry("targetUri", "http://order-service:8082"),
                Map.entry("stripPrefix", 0),
                Map.entry("order", 100),
                Map.entry("enabled", enabled),
                Map.entry("authentication", java.util.List.of("JWT")),
                Map.entry("requiredRoles", java.util.List.of()),
                Map.entry("requiredScopes", java.util.List.of("orders:read")),
                Map.entry("signingRequired", false),
                Map.entry("connectTimeoutMs", 1000),
                Map.entry("responseTimeoutMs", 3000),
                Map.entry("retryPolicy", Map.of(
                        "enabled", true, "maxAttempts", 2, "eligibleMethods", java.util.List.of("GET"))),
                Map.entry("circuitBreaker", Map.of("enabled", true, "name", "orders")),
                Map.entry("auditMode", "DENIALS_AND_MUTATIONS"),
                Map.entry("version", version));
    }
}
