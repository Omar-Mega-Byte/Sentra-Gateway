package com.omar.sentra.user.common.error;

import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * Creates the documented user-service failures without exposing submitted data.
 */
public final class ServiceErrors {
    private ServiceErrors() {}

    public static UserServiceException requestInvalid(List<ErrorDetail> details) {
        return new UserServiceException(
                HttpStatus.BAD_REQUEST,
                "USR_REQUEST_INVALID",
                "The request is invalid.",
                details);
    }

    public static UserServiceException trustedContextRequired() {
        return new UserServiceException(
                HttpStatus.UNAUTHORIZED,
                "USR_TRUSTED_CONTEXT_REQUIRED",
                "Trusted gateway context is required.");
    }

    public static UserServiceException actorNotAllowed() {
        return new UserServiceException(
                HttpStatus.FORBIDDEN,
                "USR_ACTOR_NOT_ALLOWED",
                "The trusted actor is not allowed for this operation.");
    }

    public static UserServiceException scopeRequired() {
        return new UserServiceException(
                HttpStatus.FORBIDDEN,
                "USR_SCOPE_REQUIRED",
                "The required scope is missing.");
    }

    public static UserServiceException profileNotFound() {
        return new UserServiceException(
                HttpStatus.NOT_FOUND,
                "USR_PROFILE_NOT_FOUND",
                "The requested profile was not found.");
    }

    public static UserServiceException versionConflict() {
        return new UserServiceException(
                HttpStatus.CONFLICT,
                "USR_VERSION_CONFLICT",
                "The profile was changed by another request.",
                List.of(new ErrorDetail(
                        "version",
                        "conflict",
                        "Refresh the profile and retry the update.")));
    }

    public static UserServiceException emailConflict() {
        return new UserServiceException(
                HttpStatus.CONFLICT,
                "USR_EMAIL_CONFLICT",
                "The email address is already associated with another profile.",
                List.of(new ErrorDetail(
                        "email",
                        "conflict",
                        "Use a different email address.")));
    }

    public static UserServiceException bodyTooLarge() {
        return new UserServiceException(
                HttpStatus.CONTENT_TOO_LARGE,
                "USR_BODY_TOO_LARGE",
                "The request body exceeds the configured limit.");
    }

    public static UserServiceException mediaTypeUnsupported() {
        return new UserServiceException(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "USR_MEDIA_TYPE_UNSUPPORTED",
                "Content-Type application/json is required.");
    }

    public static UserServiceException dependencyUnavailable() {
        return new UserServiceException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "USR_DEPENDENCY_UNAVAILABLE",
                "The profile repository is temporarily unavailable.");
    }
}
