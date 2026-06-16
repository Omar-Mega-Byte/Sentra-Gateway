package com.omar.sentra.gateway.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.omar.sentra.gateway.config.SentraProperties;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class RouteServiceTest {
    private RouteService service;

    @BeforeEach
    void setUp() {
        SentraProperties properties = new SentraProperties();
        properties.getRouting().setAllowedServiceHosts(List.of("order-service", "localhost"));
        service = new RouteService(mock(RouteRepository.class), properties, mock(ApplicationEventPublisher.class));
    }

    @Test
    void acceptsSafeUserRoute() {
        assertThat(service.validate(route("orders", "/api/v1/orders", "http://order-service:8082")).valid())
                .isTrue();
    }

    @Test
    void rejectsReservedAdminPath() {
        RouteValidationResult result =
                service.validate(route("bad-admin", "/api/v1/admin/steal", "http://order-service:8082"));
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(error -> error.contains("reserved"));
    }

    @Test
    void rejectsUnapprovedTarget() {
        RouteValidationResult result =
                service.validate(route("metadata", "/api/v1/orders", "http://169.254.169.254/latest"));
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(error -> error.contains("allowlisted"));
    }

    private RouteRequest route(String id, String path, String target) {
        return new RouteRequest(
                id, RouteCategory.USER, List.of(path), List.of("GET"), target, 0, 100, true,
                List.of("JWT"), List.of(), List.of("orders:read"), false, null, null, null,
                1000, 3000, new RouteRequest.RetryPolicy(true, 2, List.of("GET")),
                new RouteRequest.CircuitBreakerPolicy(true, "orders"), "DENIALS_AND_MUTATIONS", 0);
    }
}
