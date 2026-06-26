package com.sentra.notification.web;

import com.sentra.notification.common.error.ApiError;
import com.sentra.notification.common.error.ErrorCode;
import com.sentra.notification.common.error.ServiceException;
import com.sentra.notification.common.request.SentraHeaders;
import com.sentra.notification.common.request.TrustedContext;
import com.sentra.notification.common.request.TrustedContextValidator;
import com.sentra.notification.config.NotificationServiceProperties;
import com.sentra.notification.fault.AdminTestService;
import com.sentra.notification.fault.FaultControlService;
import com.sentra.notification.notification.Channel;
import com.sentra.notification.notification.NotificationPage;
import com.sentra.notification.notification.NotificationQueryService;
import com.sentra.notification.notification.NotificationStatus;
import com.sentra.notification.preference.NotificationPreferences;
import com.sentra.notification.preference.PreferenceService;
import com.sentra.notification.preference.PreferenceUpdateCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Internal notification APIs consumed only through Sentra Gateway rewrites.
 */
@Validated
@RestController
@RequestMapping(path = "/internal/v1", produces = "application/json;charset=UTF-8")
@Tag(name = "Notification Service Internal API", description = "Internal routes called by the gateway after JWT, retry, timeout, circuit, and fallback policy are applied externally.")
public class NotificationController {
    private static final MediaType JSON_UTF8 = MediaType.parseMediaType("application/json;charset=UTF-8");
    private static final String OP_LIST = NotificationServiceProperties.ROUTE_NOTIFICATIONS_LIST;
    private static final String OP_PREFERENCES = NotificationServiceProperties.ROUTE_PREFERENCES_UPDATE;
    private static final String OP_ADMIN = NotificationServiceProperties.ROUTE_ADMIN_TEST;
    private static final Set<String> LIST_QUERY_PARAMS = Set.of("page", "size", "channel", "status");
    private final TrustedContextValidator trustedContext;
    private final FaultControlService faults;
    private final NotificationQueryService notifications;
    private final PreferenceService preferences;
    private final AdminTestService adminTests;
    private final NotificationServiceProperties properties;
    private final JsonMapper jsonMapper;
    private final Validator validator;

    /**
     * Creates the internal notification controller.
     */
    public NotificationController(
            TrustedContextValidator trustedContext,
            FaultControlService faults,
            NotificationQueryService notifications,
            PreferenceService preferences,
            AdminTestService adminTests,
            NotificationServiceProperties properties,
            JsonMapper jsonMapper,
            Validator validator) {
        this.trustedContext = trustedContext;
        this.faults = faults;
        this.notifications = notifications;
        this.preferences = preferences;
        this.adminTests = adminTests;
        this.properties = properties;
        this.jsonMapper = jsonMapper;
        this.validator = validator;
    }

    /**
     * Returns notifications owned by the trusted tenant and subject.
     */
    @Operation(summary = "List trusted user's notifications", description = "Returns deterministic notifications for the trusted tenant/subject context. The gateway owns external JWT validation, retry, timeout, circuit breaker, and fallback behavior.")
    @Parameters({
            @Parameter(name = SentraHeaders.REQUEST_ID, in = ParameterIn.HEADER, required = true, example = "local-notifications-read-001", description = "Gateway-approved request ID."),
            @Parameter(name = SentraHeaders.SUBJECT, in = ParameterIn.HEADER, required = true, example = "sentra-user-omar", description = "Trusted subject forwarded by the gateway."),
            @Parameter(name = SentraHeaders.ACTOR_TYPE, in = ParameterIn.HEADER, required = true, example = "USER", description = "Trusted actor type. Must be USER."),
            @Parameter(name = SentraHeaders.TENANT_ID, in = ParameterIn.HEADER, example = "tenant-demo", description = "Trusted tenant identifier when applicable."),
            @Parameter(name = SentraHeaders.SCOPES, in = ParameterIn.HEADER, required = true, example = "notifications:read", description = "Trusted scopes. Must include notifications:read."),
            @Parameter(name = SentraHeaders.ROUTE_ID, in = ParameterIn.HEADER, required = true, example = "notifications-list", description = "Exact trusted route ID."),
            @Parameter(name = SentraHeaders.TEST_DELAY_MILLIS, in = ParameterIn.HEADER, example = "50", description = "Local/test-only bounded delay control."),
            @Parameter(name = SentraHeaders.TEST_STATUS, in = ParameterIn.HEADER, example = "503", description = "Local/test-only bounded failure status control."),
            @Parameter(name = SentraHeaders.TEST_MALFORMED, in = ParameterIn.HEADER, example = "true", description = "Local/test-only malformed response control."),
            @Parameter(name = SentraHeaders.TEST_DISCONNECT, in = ParameterIn.HEADER, example = "true", description = "Local/test-only disconnect simulation control.")
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notifications returned.", content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid query parameter.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Missing or malformed trusted context.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Actor, route, scope, or local/test fault control denied.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "503", description = "Repository unavailable.", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/notifications")
    public ResponseEntity<PageResponse<NotificationResponse>> listNotifications(
            @Parameter(description = "Zero-based page number.", example = "0") @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size.", example = "20") @RequestParam(required = false) Integer size,
            @Parameter(description = "Optional channel filter.", example = "EMAIL") @RequestParam(required = false) String channel,
            @Parameter(description = "Optional status filter.", example = "SENT") @RequestParam(required = false) String status,
            HttpServletRequest request) {
        TrustedContext context = trustedContext.requireUser(request, OP_LIST, "notifications:read");
        faults.applyHeaderFaults(request, OP_LIST);
        rejectUnknownQueryParams(request.getParameterMap());
        NotificationPage pageResult = notifications.list(
                context.tenantId(),
                context.subject(),
                page == null ? 0 : page,
                size == null ? properties.limits().defaultPageSize() : size,
                parseEnum(Channel.class, channel),
                parseEnum(NotificationStatus.class, status));
        PageResponse<NotificationResponse> body = new PageResponse<>(
                pageResult.page(),
                pageResult.size(),
                pageResult.totalElements(),
                pageResult.totalPages(),
                pageResult.items().stream().map(NotificationResponse::from).toList());
        return jsonResponse(body);
    }

    /**
     * Updates trusted user's notification preferences.
     */
    @Operation(summary = "Update trusted user's notification preferences", description = "Updates deterministic preferences using optimistic versioning. Gateway automatic retry is disabled for this mutation.")
    @Parameters({
            @Parameter(name = SentraHeaders.REQUEST_ID, in = ParameterIn.HEADER, required = true, example = "local-preferences-update-001"),
            @Parameter(name = SentraHeaders.SUBJECT, in = ParameterIn.HEADER, required = true, example = "sentra-user-omar"),
            @Parameter(name = SentraHeaders.ACTOR_TYPE, in = ParameterIn.HEADER, required = true, example = "USER"),
            @Parameter(name = SentraHeaders.TENANT_ID, in = ParameterIn.HEADER, example = "tenant-demo"),
            @Parameter(name = SentraHeaders.SCOPES, in = ParameterIn.HEADER, required = true, example = "notifications:write"),
            @Parameter(name = SentraHeaders.ROUTE_ID, in = ParameterIn.HEADER, required = true, example = "notification-preferences-update"),
            @Parameter(name = SentraHeaders.TEST_DELAY_MILLIS, in = ParameterIn.HEADER, example = "50", description = "Local/test-only bounded delay control."),
            @Parameter(name = SentraHeaders.TEST_STATUS, in = ParameterIn.HEADER, example = "503", description = "Local/test-only bounded failure status control."),
            @Parameter(name = SentraHeaders.TEST_MALFORMED, in = ParameterIn.HEADER, example = "true", description = "Local/test-only malformed response control."),
            @Parameter(name = SentraHeaders.TEST_DISCONNECT, in = ParameterIn.HEADER, example = "true", description = "Local/test-only disconnect simulation control.")
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Preferences updated.", content = @Content(schema = @Schema(implementation = PreferenceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Malformed JSON, unknown field, missing field, or invalid value.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Missing or malformed trusted context.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Actor, route, scope, or local/test fault control denied.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Optimistic version conflict.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "413", description = "Body exceeds configured limit.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "415", description = "Unsupported media type.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "503", description = "Repository unavailable.", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping(path = "/preferences", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PreferenceResponse> updatePreferences(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(schema = @Schema(implementation = PreferenceUpdateRequest.class), examples = @ExampleObject(value = "{\"emailEnabled\":true,\"smsEnabled\":false,\"pushEnabled\":true,\"webhookEnabled\":false,\"version\":2}")))
            @RequestBody(required = false) String rawBody,
            HttpServletRequest request) {
        TrustedContext context = trustedContext.requireUser(request, OP_PREFERENCES, "notifications:write");
        faults.applyHeaderFaults(request, OP_PREFERENCES);
        PreferenceUpdateRequest body = parseAndValidate(rawBody, PreferenceUpdateRequest.class);
        NotificationPreferences updated = preferences.update(context.tenantId(), context.subject(), new PreferenceUpdateCommand(
                body.emailEnabled(), body.smsEnabled(), body.pushEnabled(), body.webhookEnabled(), body.version()));
        return jsonResponse(PreferenceResponse.from(updated));
    }

    /**
     * Executes deterministic admin-only test-notification behavior.
     */
    @Operation(summary = "Run admin notification resilience test", description = "Triggers deterministic SUCCESS, DELAY, FAILURE, MALFORMED, or DISCONNECT behavior. The gateway owns circuit-breaker, timeout, fallback, and retry policy.")
    @Parameters({
            @Parameter(name = SentraHeaders.REQUEST_ID, in = ParameterIn.HEADER, required = true, example = "local-admin-test-001"),
            @Parameter(name = SentraHeaders.SUBJECT, in = ParameterIn.HEADER, required = true, example = "sentra-admin"),
            @Parameter(name = SentraHeaders.ACTOR_TYPE, in = ParameterIn.HEADER, required = true, example = "USER"),
            @Parameter(name = SentraHeaders.ROLES, in = ParameterIn.HEADER, required = true, example = "NOTIFICATION_ADMIN"),
            @Parameter(name = SentraHeaders.ROUTE_ID, in = ParameterIn.HEADER, required = true, example = "admin-test-notification"),
            @Parameter(name = SentraHeaders.TEST_DELAY_MILLIS, in = ParameterIn.HEADER, example = "50", description = "Local/test-only bounded delay control."),
            @Parameter(name = SentraHeaders.TEST_STATUS, in = ParameterIn.HEADER, example = "503", description = "Local/test-only bounded failure status control."),
            @Parameter(name = SentraHeaders.TEST_MALFORMED, in = ParameterIn.HEADER, example = "true", description = "Local/test-only malformed response control."),
            @Parameter(name = SentraHeaders.TEST_DISCONNECT, in = ParameterIn.HEADER, example = "true", description = "Local/test-only disconnect simulation control.")
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Admin test accepted, or intentionally malformed in MALFORMED scenarios.", content = @Content(schema = @Schema(implementation = AdminTestResponse.class))),
            @ApiResponse(responseCode = "400", description = "Malformed JSON, unknown field, missing field, or invalid scenario.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Missing or malformed trusted context.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Actor, route, role, or local/test fault control denied.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "413", description = "Body exceeds configured limit.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "415", description = "Unsupported media type.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Configured local/test failure or disconnect simulation.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "502", description = "Configured local/test failure.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "503", description = "Configured local/test failure or dependency unavailable.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "504", description = "Configured local/test failure.", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping(path = "/test", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AdminTestResponse> adminTest(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(schema = @Schema(implementation = AdminTestRequest.class), examples = @ExampleObject(value = "{\"scenario\":\"SUCCESS\",\"channel\":\"EMAIL\",\"recipientReference\":\"test-recipient\",\"message\":\"Gateway resilience smoke test\"}")))
            @RequestBody(required = false) String rawBody,
            HttpServletRequest request) {
        trustedContext.requireAdmin(request, OP_ADMIN, "NOTIFICATION_ADMIN");
        faults.applyHeaderFaults(request, OP_ADMIN);
        AdminTestRequest body = parseAndValidate(rawBody, AdminTestRequest.class);
        return jsonResponse(adminTests.execute(request, body, OP_ADMIN));
    }

    private void rejectUnknownQueryParams(Map<String, String[]> parameters) {
        Optional<String> unknown = parameters.keySet().stream()
                .filter(parameter -> !LIST_QUERY_PARAMS.contains(parameter))
                .findFirst();
        if (unknown.isPresent()) {
            throw ServiceException.withDetails(ErrorCode.NTF_REQUEST_INVALID, List.of("query: unknown parameter"));
        }
    }

    private <T> T parseAndValidate(String rawBody, Class<T> type) {
        if (rawBody == null || rawBody.isBlank()) {
            throw ServiceException.of(ErrorCode.NTF_REQUEST_INVALID);
        }
        T body;
        try {
            body = jsonMapper.readValue(rawBody, type);
        } catch (JacksonException exception) {
            throw ServiceException.of(ErrorCode.NTF_REQUEST_INVALID);
        }
        Set<ConstraintViolation<T>> violations = validator.validate(body);
        if (!violations.isEmpty()) {
            List<String> details = violations.stream()
                    .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                    .toList();
            throw ServiceException.withDetails(ErrorCode.NTF_REQUEST_INVALID, details);
        }
        return body;
    }

    private <E extends Enum<E>> Optional<E> parseEnum(Class<E> type, String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Enum.valueOf(type, value));
        } catch (IllegalArgumentException exception) {
            throw ServiceException.of(ErrorCode.NTF_REQUEST_INVALID);
        }
    }

    private <T> ResponseEntity<T> jsonResponse(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(JSON_UTF8).body(body);
    }
}
