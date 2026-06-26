package com.omar.sentra.user.web;

import static com.omar.sentra.user.common.error.ServiceErrors.profileNotFound;
import static com.omar.sentra.user.common.error.ServiceErrors.requestInvalid;

import com.omar.sentra.user.common.error.ApiError;
import com.omar.sentra.user.common.error.ErrorDetail;
import com.omar.sentra.user.common.request.TrustedContextResolver;
import com.omar.sentra.user.common.request.TrustedRequestContext;
import com.omar.sentra.user.config.UserServiceProperties;
import com.omar.sentra.user.profile.CurrentProfileResponse;
import com.omar.sentra.user.profile.ProfilePatchRequest;
import com.omar.sentra.user.profile.ProfileService;
import com.omar.sentra.user.profile.PublicProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal profile endpoints called by the trusted gateway.
 */
@RestController
@RequestMapping(path = "/internal/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Profiles", description = "Public and trusted current-user profile operations")
public class ProfileController {
    private final ProfileService profileService;
    private final TrustedContextResolver contextResolver;
    private final UserServiceProperties properties;

    public ProfileController(
            ProfileService profileService,
            TrustedContextResolver contextResolver,
            UserServiceProperties properties) {
        this.profileService = profileService;
        this.contextResolver = contextResolver;
        this.properties = properties;
    }

    /**
     * Returns the allowlisted public fields for an active profile.
     *
     * @param id canonical UUID string
     * @param request servlet request containing trusted gateway provenance
     * @return public profile response
     */
    @GetMapping("/{id}/public")
    @Operation(
            summary = "Get a public profile",
            description = "Returns only the four allowlisted public fields for an active profile.")
    @Parameters({
        @Parameter(
                name = "X-Sentra-Request-Id",
                description = "Gateway-approved visible ASCII request ID, maximum 128 characters",
                required = true,
                example = "8e3a95b8-6674-423e-83e6-0df84c2d66d0"),
        @Parameter(
                name = "X-Sentra-Route-Id",
                description = "Must be user-public-profile",
                required = true,
                example = "user-public-profile")
    })
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Active public profile",
                content = @Content(schema = @Schema(implementation = PublicProfileResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid canonical UUID",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Missing or malformed trusted provenance",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Unknown, disabled, deleted, or unavailable public profile",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PublicProfileResponse> publicProfile(
            @Parameter(
                            description = "Canonical lowercase profile UUID",
                            required = true,
                            example = "7aa99db8-a943-4b63-b4b7-79f769ef9f87")
                    @PathVariable
                    String id,
            HttpServletRequest request) {
        contextResolver.requirePublic(request);
        if (!properties.publicProfile().enabled()) {
            throw profileNotFound();
        }
        UUID profileId = canonicalUuid(id);
        return ResponseEntity.ok()
                .header("Cache-Control", properties.publicProfile().cacheControl())
                .body(profileService.publicProfile(profileId));
    }

    /**
     * Returns the safe private profile for the trusted subject.
     *
     * @param request servlet request containing trusted user context
     * @return current profile response
     */
    @GetMapping("/me")
    @Operation(
            summary = "Get the current profile",
            description = "Resolves identity exclusively from X-Sentra-Subject and requires profile:read.")
    @Parameters({
        @Parameter(name = "X-Sentra-Request-Id", required = true, example = "8e3a95b8-6674-423e-83e6-0df84c2d66d0"),
        @Parameter(name = "X-Sentra-Subject", required = true, example = "sentra-user-omar"),
        @Parameter(name = "X-Sentra-Actor-Type", required = true, example = "USER"),
        @Parameter(name = "X-Sentra-Scopes", required = true, example = "profile:read"),
        @Parameter(name = "X-Sentra-Route-Id", required = true, example = "user-profile-read"),
        @Parameter(name = "X-Sentra-Tenant-Id", required = false),
        @Parameter(name = "X-Sentra-Roles", required = false, example = "USER"),
        @Parameter(name = "X-Sentra-Source-Ip", required = false, example = "203.0.113.10")
    })
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Safe current profile",
                content = @Content(schema = @Schema(implementation = CurrentProfileResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Missing or malformed trusted context",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Actor is not USER or profile:read is absent",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Trusted subject has no active profile",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "503",
                description = "Required profile repository unavailable",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<CurrentProfileResponse> currentProfile(HttpServletRequest request) {
        TrustedRequestContext context = contextResolver.requireUser(
                request,
                TrustedContextResolver.READ_ROUTE,
                "profile:read");
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(profileService.currentProfile(context.subject()));
    }

    /**
     * Applies a merge-style update to the trusted subject's active profile.
     *
     * @param patch requested mutable values and optimistic version
     * @param request servlet request containing trusted user context
     * @return updated or unchanged current profile
     */
    @PatchMapping(path = "/me", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Update the current profile",
            description = """
                    Applies merge-style field-presence semantics and requires profile:write.
                    Omitted fields remain unchanged; only bio and avatarUrl accept explicit null.
                    """)
    @Parameters({
        @Parameter(name = "X-Sentra-Request-Id", required = true, example = "8e3a95b8-6674-423e-83e6-0df84c2d66d0"),
        @Parameter(name = "X-Sentra-Subject", required = true, example = "sentra-user-omar"),
        @Parameter(name = "X-Sentra-Actor-Type", required = true, example = "USER"),
        @Parameter(name = "X-Sentra-Scopes", required = true, example = "profile:write"),
        @Parameter(name = "X-Sentra-Route-Id", required = true, example = "user-profile-update"),
        @Parameter(name = "X-Sentra-Tenant-Id", required = false),
        @Parameter(name = "X-Sentra-Roles", required = false, example = "USER"),
        @Parameter(name = "X-Sentra-Source-Ip", required = false, example = "203.0.113.10")
    })
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Updated profile or valid no-op",
                content = @Content(schema = @Schema(implementation = CurrentProfileResponse.class))),
        @ApiResponse(responseCode = "400", description = "Malformed JSON, unknown field, or invalid value",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Missing or malformed trusted context",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Actor is not USER or profile:write is absent",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Trusted subject has no active profile",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Version or normalized email conflict",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "413", description = "Request body exceeds the configured limit",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "415", description = "Content-Type is not application/json",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "503", description = "Required profile repository unavailable",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<CurrentProfileResponse> updateCurrentProfile(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            description = "Mutable fields plus the current optimistic version",
                            content = @Content(
                                    schema = @Schema(implementation = ProfilePatchRequest.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "displayName": "Omar H.",
                                              "bio": "Building secure Java services",
                                              "avatarUrl": null,
                                              "email": "omar.h@example.test",
                                              "locale": "en-EG",
                                              "timezone": "Africa/Cairo",
                                              "version": 3
                                            }
                                            """)))
                    @RequestBody
                    ProfilePatchRequest patch,
            HttpServletRequest request) {
        TrustedRequestContext context = contextResolver.requireUser(
                request,
                TrustedContextResolver.UPDATE_ROUTE,
                "profile:write");
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(profileService.updateCurrentProfile(context.subject(), patch));
    }

    private static UUID canonicalUuid(String value) {
        try {
            UUID id = UUID.fromString(value);
            if (!id.toString().equals(value)) {
                throw new IllegalArgumentException("Non-canonical UUID.");
            }
            return id;
        } catch (IllegalArgumentException exception) {
            throw requestInvalid(List.of(new ErrorDetail(
                    "id",
                    "format",
                    "Profile ID must be a canonical UUID.")));
        }
    }
}
