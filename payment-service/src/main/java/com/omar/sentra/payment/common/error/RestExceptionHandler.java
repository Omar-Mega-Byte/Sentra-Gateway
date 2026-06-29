package com.omar.sentra.payment.common.error;

import com.omar.sentra.payment.common.request.RequestAttributes;
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
 * Converts expected and unexpected failures to the stable payment error
 * contract.
 */
@RestControllerAdvice
public class RestExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    private final ApiErrorFactory errorFactory;

    public RestExceptionHandler(ApiErrorFactory errorFactory) {
        this.errorFactory = errorFactory;
    }

    @ExceptionHandler(PaymentServiceException.class)
    ResponseEntity<ApiError> handleServiceException(PaymentServiceException exception, HttpServletRequest request) {
        return response(exception, request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiError> handleUnsupportedMedia(HttpMediaTypeNotSupportedException exception, HttpServletRequest request) {
        return response(ServiceErrors.mediaTypeUnsupported(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException exception, HttpServletRequest request) {
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
                new PaymentServiceException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "PAY_INTERNAL_ERROR",
                        "An unexpected internal error occurred."),
                request);
    }

    private ResponseEntity<ApiError> response(PaymentServiceException exception, HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noStore());
        headers.setContentType(MediaType.parseMediaType("application/json;charset=UTF-8"));
        return new ResponseEntity<>(errorFactory.create(request, exception), headers, exception.status());
    }

    private static String normalizedOperation(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.matches("/internal/v1/payments/[^/]+")) {
            return "payment-read";
        }
        if ("/internal/v1/payments".equals(path)) {
            return "payment-create";
        }
        if ("/internal/v1/refunds".equals(path)) {
            return "refund-create";
        }
        return "unmapped";
    }
}
