package com.omar.sentra.user;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.omar.sentra.user.common.request.TrustedHeaders;
import com.omar.sentra.user.profile.ProfileRepository;
import com.omar.sentra.user.profile.ProfileSeedData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserServiceApplicationTest {
    private static final String REQUEST_ID = "8e3a95b8-6674-423e-83e6-0df84c2d66d0";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProfileRepository repository;

    @BeforeEach
    void resetRepository() {
        repository.reset();
    }

    @Test
    void publicProfileIsRedactedAndCorrelated() throws Exception {
        mockMvc.perform(publicHeaders(get("/internal/v1/users/{id}/public", ProfileSeedData.ACTIVE_ID)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", REQUEST_ID))
                .andExpect(header().string("Cache-Control", "public, max-age=60"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(ProfileSeedData.ACTIVE_ID.toString()))
                .andExpect(jsonPath("$.displayName").value("Omar Hassan"))
                .andExpect(jsonPath("$.bio").value("Backend engineer"))
                .andExpect(jsonPath("$.avatarUrl").exists())
                .andExpect(jsonPath("$.subject").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.version").doesNotExist())
                .andExpect(jsonPath("$.*", hasSize(4)));
    }

    @Test
    void publicProfileRequiresProvenanceAndCanonicalUuid() throws Exception {
        mockMvc.perform(get("/internal/v1/users/{id}/public", ProfileSeedData.ACTIVE_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("USR_TRUSTED_CONTEXT_REQUIRED"))
                .andExpect(header().exists("X-Request-Id"));

        mockMvc.perform(publicHeaders(get(
                        "/internal/v1/users/{id}/public",
                        ProfileSeedData.ACTIVE_ID.toString().toUpperCase())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USR_REQUEST_INVALID"))
                .andExpect(jsonPath("$.details[0].field").value("id"));
    }

    @Test
    void publicLookupHidesDisabledDeletedAndUnknownProfiles() throws Exception {
        for (String id : new String[] {
            "11111111-1111-4111-8111-111111111111",
            "22222222-2222-4222-8222-222222222222",
            "33333333-3333-4333-8333-333333333333"
        }) {
            mockMvc.perform(publicHeaders(get("/internal/v1/users/{id}/public", id)))
                    .andExpect(status().isNotFound())
                    .andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(jsonPath("$.code").value("USR_PROFILE_NOT_FOUND"));
        }
    }

    @Test
    void currentProfileUsesTrustedSubjectAndOmitsInternalFields() throws Exception {
        mockMvc.perform(readHeaders(get("/internal/v1/users/me")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.id").value(ProfileSeedData.ACTIVE_ID.toString()))
                .andExpect(jsonPath("$.email").value("omar@example.test"))
                .andExpect(jsonPath("$.version").value(3))
                .andExpect(jsonPath("$.subject").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.scopes").doesNotExist());
    }

    @Test
    void currentProfileRejectsMissingDuplicateOrContradictoryContext() throws Exception {
        mockMvc.perform(get("/internal/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("USR_TRUSTED_CONTEXT_REQUIRED"));

        mockMvc.perform(readHeaders(get("/internal/v1/users/me"))
                        .header(TrustedHeaders.SUBJECT, ProfileSeedData.ACTIVE_SUBJECT, "another-subject"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("USR_TRUSTED_CONTEXT_REQUIRED"));

        mockMvc.perform(readHeaders(get("/internal/v1/users/me"))
                        .header(TrustedHeaders.CLIENT_ID, "api-client"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USR_ACTOR_NOT_ALLOWED"));
    }

    @Test
    void currentProfileRejectsInvalidActorAndMissingScope() throws Exception {
        mockMvc.perform(baseUserHeaders(get("/internal/v1/users/me"), "API_CLIENT", "profile:read", "user-profile-read"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USR_ACTOR_NOT_ALLOWED"));

        mockMvc.perform(baseUserHeaders(get("/internal/v1/users/me"), "USER", "orders:read", "user-profile-read"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USR_SCOPE_REQUIRED"));
    }

    @Test
    void patchUpdatesOnceAndSupportsValidNoOp() throws Exception {
        mockMvc.perform(writeHeaders(patch("/internal/v1/users/me"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "  Omar H.  ",
                                  "bio": "Building secure Java services",
                                  "email": "OMAR.H@EXAMPLE.TEST",
                                  "version": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Omar H."))
                .andExpect(jsonPath("$.email").value("omar.h@example.test"))
                .andExpect(jsonPath("$.version").value(4));

        repository.reset();
        mockMvc.perform(writeHeaders(patch("/internal/v1/users/me"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Omar Hassan","version":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(3));
    }

    @Test
    void patchRejectsStaleVersionAndEmailConflict() throws Exception {
        mockMvc.perform(writeHeaders(patch("/internal/v1/users/me"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Changed\",\"version\":2}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USR_VERSION_CONFLICT"))
                .andExpect(jsonPath("$.details[0].field").value("version"));

        mockMvc.perform(writeHeaders(patch("/internal/v1/users/me"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"disabled@example.test\",\"version\":3}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USR_EMAIL_CONFLICT"));

        mockMvc.perform(writeHeaders(patch("/internal/v1/users/me"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Omar Hassan\",\"version\":2}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USR_VERSION_CONFLICT"));
    }

    @Test
    void patchRejectsUnknownImmutableMalformedAndInvalidFields() throws Exception {
        for (String body : new String[] {
            "{\"id\":\"7aa99db8-a943-4b63-b4b7-79f769ef9f87\",\"version\":3}",
            "{\"unknown\":true,\"version\":3}",
            "{\"displayName\":null,\"version\":3}",
            "{\"avatarUrl\":\"https://localhost/avatar.png\",\"version\":3}",
            "{\"version\":3}",
            "{\"displayName\":\"Changed\",\"version\":"
        }) {
            mockMvc.perform(writeHeaders(patch("/internal/v1/users/me"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("USR_REQUEST_INVALID"));
        }
    }

    @Test
    void patchEnforcesContentTypeAndBodyLimit() throws Exception {
        mockMvc.perform(writeHeaders(patch("/internal/v1/users/me"))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("{\"displayName\":\"Changed\",\"version\":3}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("USR_MEDIA_TYPE_UNSUPPORTED"));

        String oversized = "{\"bio\":\"" + "x".repeat(17000) + "\",\"version\":3}";
        mockMvc.perform(writeHeaders(patch("/internal/v1/users/me"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(oversized))
                .andExpect(status().isContentTooLarge())
                .andExpect(jsonPath("$.code").value("USR_BODY_TOO_LARGE"));
    }

    @Test
    void healthMetricsAndSwaggerAreAvailableInTestProfile() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(publicHeaders(get("/internal/v1/users/{id}/public", ProfileSeedData.ACTIVE_ID)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("sentra_user_profile_lookups_total")));

        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));
    }

    @Test
    void openApiContainsEveryPathHeaderSchemaAndResponse() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Sentra User Service Internal API"))
                .andExpect(jsonPath("$.paths['/internal/v1/users/{id}/public'].get").exists())
                .andExpect(jsonPath("$.paths['/internal/v1/users/me'].get").exists())
                .andExpect(jsonPath("$.paths['/internal/v1/users/me'].patch").exists())
                .andExpect(jsonPath("$.paths['/internal/v1/users/me'].patch.responses['413']").exists())
                .andExpect(jsonPath("$.paths['/internal/v1/users/me'].patch.responses['415']").exists())
                .andExpect(jsonPath("$.components.schemas.ApiError").exists())
                .andExpect(content().string(containsString("X-Sentra-Subject")))
                .andExpect(content().string(containsString("profile:write")));
    }

    private static MockHttpServletRequestBuilder publicHeaders(MockHttpServletRequestBuilder builder) {
        return builder.header(TrustedHeaders.REQUEST_ID, REQUEST_ID)
                .header(TrustedHeaders.ROUTE_ID, "user-public-profile");
    }

    private static MockHttpServletRequestBuilder readHeaders(MockHttpServletRequestBuilder builder) {
        return baseUserHeaders(builder, "USER", "profile:read", "user-profile-read");
    }

    private static MockHttpServletRequestBuilder writeHeaders(MockHttpServletRequestBuilder builder) {
        return baseUserHeaders(builder, "USER", "profile:write", "user-profile-update");
    }

    private static MockHttpServletRequestBuilder baseUserHeaders(
            MockHttpServletRequestBuilder builder,
            String actor,
            String scopes,
            String routeId) {
        return builder.header(TrustedHeaders.REQUEST_ID, REQUEST_ID)
                .header(TrustedHeaders.SUBJECT, ProfileSeedData.ACTIVE_SUBJECT)
                .header(TrustedHeaders.ACTOR_TYPE, actor)
                .header(TrustedHeaders.SCOPES, scopes)
                .header(TrustedHeaders.ROUTE_ID, routeId);
    }
}
