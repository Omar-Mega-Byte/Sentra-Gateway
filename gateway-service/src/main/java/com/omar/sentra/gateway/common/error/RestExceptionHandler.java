package com.omar.sentra.gateway.common.error;

import com.omar.sentra.gateway.common.request.RequestAttributes;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;

/**
 * Maps controller validation and service exceptions to the gateway schema.
 */
@RestControllerAdvice
public class RestExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(GatewayException.class)
    public ResponseEntity<ApiError> gateway(GatewayException exception, ServerWebExchange exchange) {
        return response(exception.errorCode(), exception.getMessage(), exception.details(), exchange);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiError> binding(WebExchangeBindException exception, ServerWebExchange exchange) {
        List<ErrorDetail> details = exception.getFieldErrors().stream()
                .map(this::detail)
                .toList();
        return response(ErrorCode.GW_REQUEST_INVALID, ErrorCode.GW_REQUEST_INVALID.defaultMessage(), details, exchange);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> constraint(
            ConstraintViolationException exception, ServerWebExchange exchange) {
        List<ErrorDetail> details = exception.getConstraintViolations().stream()
                .map(violation -> new ErrorDetail(
                        violation.getPropertyPath().toString(), "INVALID", violation.getMessage()))
                .toList();
        return response(ErrorCode.GW_REQUEST_INVALID, ErrorCode.GW_REQUEST_INVALID.defaultMessage(), details, exchange);
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ApiError> unexpected(Throwable exception, ServerWebExchange exchange) {
        LOGGER.error(
                "Unhandled request failure for {} {}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath(),
                exception);
        return response(
                ErrorCode.GW_INTERNAL_ERROR, ErrorCode.GW_INTERNAL_ERROR.defaultMessage(), List.of(), exchange);
    }

    private ResponseEntity<ApiError> response(
            ErrorCode code, String message, List<ErrorDetail> details, ServerWebExchange exchange) {
        exchange.getAttributes().put(RequestAttributes.DECISION, "DENY");
        exchange.getAttributes().put(RequestAttributes.REASON_CODE, code.name());
        return ResponseEntity.status(code.status()).body(new ApiError(
                Instant.now(),
                exchange.getAttributeOrDefault(RequestAttributes.REQUEST_ID, "unknown"),
                code.status().value(),
                code.name(),
                message,
                exchange.getRequest().getPath().value(),
                null,
                details));
    }

    private ErrorDetail detail(FieldError error) {
        return new ErrorDetail(error.getField(), "INVALID", error.getDefaultMessage());
    }
}
