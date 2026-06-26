package com.sentra.notification.common.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sentra.notification.common.error.ErrorCode;
import com.sentra.notification.common.error.ServiceException;
import com.sentra.notification.config.NotificationServiceProperties;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;

class TrustedContextValidatorTest {

    @Test
    void productionPeerAllowlistAcceptsConfiguredCidr() {
        TrustedContextValidator validator = validator(List.of("10.20.0.0/16"));
        MockHttpServletRequest request = userRequest("10.20.4.9");

        assertThatCode(() -> validator.requireUser(request, "notifications-list", "notifications:read"))
                .doesNotThrowAnyException();
    }

    @Test
    void productionPeerAllowlistRejectsOutsideCidr() {
        TrustedContextValidator validator = validator(List.of("10.20.0.0/16"));
        MockHttpServletRequest request = userRequest("10.21.4.9");

        assertThatThrownBy(() -> validator.requireUser(request, "notifications-list", "notifications:read"))
                .isInstanceOf(ServiceException.class)
                .satisfies(error -> assertThat(((ServiceException) error).code())
                        .isEqualTo(ErrorCode.NTF_TRUSTED_CONTEXT_REQUIRED));
    }

    private static TrustedContextValidator validator(List<String> allowedPeers) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        return new TrustedContextValidator(properties(allowedPeers), environment);
    }

    private static MockHttpServletRequest userRequest(String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/v1/notifications");
        request.setRemoteAddr(remoteAddress);
        request.addHeader(SentraHeaders.REQUEST_ID, "request-123");
        request.addHeader(SentraHeaders.SUBJECT, "sentra-user-omar");
        request.addHeader(SentraHeaders.ACTOR_TYPE, "USER");
        request.addHeader(SentraHeaders.TENANT_ID, "tenant-demo");
        request.addHeader(SentraHeaders.SCOPES, "notifications:read");
        request.addHeader(SentraHeaders.ROUTE_ID, "notifications-list");
        return request;
    }

    private static NotificationServiceProperties properties(List<String> allowedPeers) {
        return new NotificationServiceProperties(
                "notification-service",
                "prod",
                "notification-service-prod",
                Duration.ofSeconds(20),
                new NotificationServiceProperties.Repository("memory", false, "default"),
                new NotificationServiceProperties.Gateway(
                        true, NotificationServiceProperties.DOCUMENTED_ROUTE_IDS, allowedPeers, 255, 128, 255, 128),
                new NotificationServiceProperties.Limits(16384, 20, 100, 10000, 120, 1000, 128, 20),
                new NotificationServiceProperties.Fault(false, false, false, false, false, 5000, List.of(500, 502, 503, 504), 1000),
                new NotificationServiceProperties.Management(List.of("10.0.0.0/8"), true, false, false),
                new NotificationServiceProperties.Logging("INFO", "json", false, false));
    }
}
