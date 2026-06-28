package com.omar.sentra.order.common.error;

import com.omar.sentra.order.common.request.RequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Converts expected and unexpected failures to the stable order error contract.
 */
@RestControllerAdvice
public class RestExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    private final ApiErrorFactory errorFactory;

    public RestExceptionHandler(ApiErrorFactory errorFactory) {
        this.errorFactory = errorFactory;
    }

    @ExceptionHandler(OrderServiceException.class)
    ResponseEntity<ApiError> handleServiceException(
            OrderServiceException exception,
            HttpServletRequest request) {
        return response(exception, request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiError> handleUnsupportedMedia(
            HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request) {
        return response(ServiceErrors.mediaTypeUnsupported(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        return response(
                ServiceErrors.requestInvalid(List.of(new ErrorDetail(
                        "body",
                        "malformed",
                        "The JSON body is malformed or contains an unknown field."))),
                request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error(
                "unexpected request failure requestId={} operation={}",
                RequestAttributes.requestId(request),
                normalizedOperation(request),
                exception);
        return response(
                new OrderServiceException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "ORD_INTERNAL_ERROR",
                        "An unexpected internal error occurred."),
                request);
    }

    private ResponseEntity<ApiError> response(
            OrderServiceException exception,
            HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noStore());
        headers.setContentType(MediaType.parseMediaType("application/json;charset=UTF-8"));
        return new ResponseEntity<>(
                errorFactory.create(request, exception),
                headers,
                exception.status());
    }

    private static String normalizedOperation(HttpServletRequest request) {
        String path = request.getRequestURI();
        if ("/internal/v1/orders".equals(path)) {
            return "POST".equals(request.getMethod()) ? "create" : "list";
        }
        if (path.matches("/internal/v1/orders/[^/]+")) {
            return "get";
        }
        if (path.matches("/internal/v1/orders/[^/]+/cancel")) {
            return "cancel";
        }
        if ("/internal/v1/admin/orders".equals(path)) {
            return "admin-list";
        }
        if (path.matches("/internal/v1/admin/orders/[^/]+")) {
            return "admin-update";
        }
        return "unmapped";
    }
}
