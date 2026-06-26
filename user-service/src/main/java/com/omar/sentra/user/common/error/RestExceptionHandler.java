package com.omar.sentra.user.common.error;

import com.omar.sentra.user.common.request.RequestAttributes;
import com.omar.sentra.user.config.UserServiceProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Converts expected and unexpected failures to the stable user-service error
 * contract.
 */
@RestControllerAdvice
public class RestExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    private final ApiErrorFactory errorFactory;
    private final UserServiceProperties properties;

    public RestExceptionHandler(ApiErrorFactory errorFactory, UserServiceProperties properties) {
        this.errorFactory = errorFactory;
        this.properties = properties;
    }

    @ExceptionHandler(UserServiceException.class)
    ResponseEntity<ApiError> handleServiceException(
            UserServiceException exception,
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
        UserServiceException invalid = ServiceErrors.requestInvalid(List.of(new ErrorDetail(
                "body",
                "malformed",
                "The JSON body is malformed or contains an unknown field.")));
        return response(invalid, request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error(
                "unexpected request failure requestId={} operation={}",
                RequestAttributes.requestId(request),
                normalizedOperation(request),
                exception);
        UserServiceException internal = new UserServiceException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "USR_INTERNAL_ERROR",
                "An unexpected internal error occurred.");
        return response(internal, request);
    }

    private ResponseEntity<ApiError> response(
            UserServiceException exception,
            HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noStore());
        if ("USR_PROFILE_NOT_FOUND".equals(exception.code())
                && request.getRequestURI().endsWith("/public")) {
            headers.set(HttpHeaders.CACHE_CONTROL, properties.publicProfile().notFoundCacheControl());
        }
        return new ResponseEntity<>(
                errorFactory.create(request, exception),
                headers,
                exception.status());
    }

    private static String normalizedOperation(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.matches("/internal/v1/users/[^/]+/public")) {
            return "public-profile-read";
        }
        if ("/internal/v1/users/me".equals(path)) {
            return "PATCH".equals(request.getMethod()) ? "profile-update" : "profile-read";
        }
        return "unmapped";
    }
}
