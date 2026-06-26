package com.sentra.notification.common.error;

import com.sentra.notification.common.request.SentraHeaders;
import com.sentra.notification.fault.DisconnectSimulationException;
import com.sentra.notification.fault.MalformedResponseException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Maps framework and domain failures into the stable documented {@code NTF_*}
 * error contract without leaking bodies, owner references, stacks, or headers.
 */
@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private final ApiErrorSupport errors;

    /** @param errors shared API error factory */
    public ApiExceptionHandler(ApiErrorSupport errors) {
        this.errors = errors;
    }

    /** Handles explicit service exceptions. */
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ApiError> handleServiceException(ServiceException exception, HttpServletRequest request) {
        return response(request, exception.code(), exception.status(), exception.details());
    }

    /** Commits intentionally malformed local/test JSON. */
    @ExceptionHandler(MalformedResponseException.class)
    public ResponseEntity<String> handleMalformed(MalformedResponseException exception, HttpServletRequest request) {
        String requestId = errors.resolveOrCreateRequestId(request);
        return ResponseEntity.status(exception.status())
                .header(SentraHeaders.RESPONSE_REQUEST_ID, requestId)
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"scenario\":\"MALFORMED\",\"accepted\":");
    }

    /** Commits a partial response with {@code Connection: close}. */
    @ExceptionHandler(DisconnectSimulationException.class)
    public void handleDisconnect(DisconnectSimulationException exception, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String requestId = errors.resolveOrCreateRequestId(request);
        response.setStatus(ErrorCode.NTF_TEST_FAILURE.status());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(SentraHeaders.RESPONSE_REQUEST_ID, requestId);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader(HttpHeaders.CONNECTION, "close");
        response.getWriter().write("{\"code\":\"NTF_TEST_FAILURE\"");
        response.flushBuffer();
    }

    /** Handles malformed JSON and unknown JSON fields. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return response(request, ErrorCode.NTF_REQUEST_INVALID, ErrorCode.NTF_REQUEST_INVALID.status(), List.of());
    }

    /** Handles unsupported media types. */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleUnsupportedMedia(HttpMediaTypeNotSupportedException exception, HttpServletRequest request) {
        return response(request, ErrorCode.NTF_MEDIA_TYPE_UNSUPPORTED, ErrorCode.NTF_MEDIA_TYPE_UNSUPPORTED.status(), List.of());
    }

    /** Handles bean validation errors from request bodies. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValid(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<String> details = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return response(request, ErrorCode.NTF_REQUEST_INVALID, ErrorCode.NTF_REQUEST_INVALID.status(), details);
    }

    /** Handles method validation errors from controller arguments. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleHandlerMethodValidation(HandlerMethodValidationException exception, HttpServletRequest request) {
        return response(request, ErrorCode.NTF_REQUEST_INVALID, ErrorCode.NTF_REQUEST_INVALID.status(), List.of());
    }

    /** Handles constraint, missing parameter, and type mismatch failures. */
    @ExceptionHandler({
            ConstraintViolationException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ApiError> handleBadRequest(Exception exception, HttpServletRequest request) {
        return response(request, ErrorCode.NTF_REQUEST_INVALID, ErrorCode.NTF_REQUEST_INVALID.status(), List.of());
    }

    /** Handles unexpected failures with a stable internal-error response. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.warn("notification-service unexpected_error requestId={} operation={} exception={}",
                errors.resolveOrCreateRequestId(request),
                request.getAttribute(SentraHeaders.ATTR_OPERATION),
                exception.getClass().getName());
        return response(request, ErrorCode.NTF_INTERNAL_ERROR, ErrorCode.NTF_INTERNAL_ERROR.status(), List.of());
    }

    private ResponseEntity<ApiError> response(HttpServletRequest request, ErrorCode code, int status, List<String> details) {
        ApiError error = errors.create(request, code, status, details);
        return ResponseEntity.status(status)
                .header(SentraHeaders.RESPONSE_REQUEST_ID, error.requestId())
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.APPLICATION_JSON)
                .body(error);
    }
}
