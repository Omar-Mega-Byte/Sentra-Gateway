package com.omar.sentra.gateway.common.error;

import com.omar.sentra.gateway.common.request.RequestAttributes;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

/**
 * Writes consistent errors for gateway filters and routing failures.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalErrorHandler implements ErrorWebExceptionHandler {
    private final ObjectMapper objectMapper;

    public GlobalErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable error) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(error);
        }
        GatewayException gatewayException = error instanceof GatewayException known
                ? known
                : new GatewayException(ErrorCode.GW_INTERNAL_ERROR);
        ErrorCode code = gatewayException.errorCode();
        exchange.getAttributes().put(RequestAttributes.DECISION, "DENY");
        exchange.getAttributes().put(RequestAttributes.REASON_CODE, code.name());
        exchange.getResponse().setStatusCode(code.status());
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        ApiError body = new ApiError(
                Instant.now(),
                exchange.getAttributeOrDefault(RequestAttributes.REQUEST_ID, "unknown"),
                code.status().value(),
                code.name(),
                gatewayException.getMessage(),
                exchange.getRequest().getPath().value(),
                null,
                gatewayException.details() == null ? List.of() : gatewayException.details());
        byte[] json = objectMapper.writeValueAsBytes(body);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(json)));
    }
}
