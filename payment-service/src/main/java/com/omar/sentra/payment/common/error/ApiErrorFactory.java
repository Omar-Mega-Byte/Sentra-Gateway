package com.omar.sentra.payment.common.error;

import com.omar.sentra.payment.common.request.RequestAttributes;
import com.omar.sentra.payment.config.PaymentServiceProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Builds bounded error responses with request and route correlation.
 */
@Component
public class ApiErrorFactory {
    private final Clock clock;
    private final PaymentServiceProperties properties;

    public ApiErrorFactory(Clock clock, PaymentServiceProperties properties) {
        this.clock = clock;
        this.properties = properties;
    }

    /**
     * Creates a stable payment-service error response.
     *
     * @param request current servlet request
     * @param exception safe service exception
     * @return API error
     */
    public ApiError create(HttpServletRequest request, PaymentServiceException exception) {
        return new ApiError(
                Instant.now(clock),
                RequestAttributes.requestId(request),
                exception.status().value(),
                exception.code(),
                exception.getMessage(),
                request.getRequestURI(),
                RequestAttributes.routeId(request),
                exception.details().stream()
                        .limit(properties.limits().maxErrorDetails())
                        .toList());
    }
}
