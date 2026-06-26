package com.omar.sentra.user.common.error;

import com.omar.sentra.user.common.request.RequestAttributes;
import com.omar.sentra.user.config.UserServiceProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Builds bounded error responses with request and route correlation.
 */
@Component
public class ApiErrorFactory {
    private final Clock clock;
    private final UserServiceProperties properties;

    public ApiErrorFactory(Clock clock, UserServiceProperties properties) {
        this.clock = clock;
        this.properties = properties;
    }

    public ApiError create(HttpServletRequest request, UserServiceException exception) {
        List<ErrorDetail> details = exception.details().stream()
                .limit(properties.limits().maxErrorDetails())
                .toList();
        return new ApiError(
                Instant.now(clock),
                RequestAttributes.requestId(request),
                exception.status().value(),
                exception.code(),
                exception.getMessage(),
                request.getRequestURI(),
                RequestAttributes.routeId(request),
                details);
    }
}
