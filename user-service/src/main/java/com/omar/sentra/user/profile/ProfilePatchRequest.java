package com.omar.sentra.user.profile;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Merge-style profile patch that preserves whether each mutable field was
 * omitted or explicitly supplied as {@code null}.
 */
@Schema(description = "Merge-style current-profile update")
public class ProfilePatchRequest {
    private String displayName;
    private boolean displayNameProvided;
    private String bio;
    private boolean bioProvided;
    private String avatarUrl;
    private boolean avatarUrlProvided;
    private String email;
    private boolean emailProvided;
    private String locale;
    private boolean localeProvided;
    private String timezone;
    private boolean timezoneProvided;
    private Long version;
    private boolean versionProvided;

    @Schema(minLength = 1, maxLength = 100, example = "Omar H.")
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        this.displayNameProvided = true;
    }

    @Schema(nullable = true, maxLength = 500, example = "Building secure Java services")
    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
        this.bioProvided = true;
    }

    @Schema(nullable = true, format = "uri", maxLength = 2048)
    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        this.avatarUrlProvided = true;
    }

    @Schema(format = "email", maxLength = 254, example = "omar.h@example.test")
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
        this.emailProvided = true;
    }

    @Schema(maxLength = 35, example = "en-EG")
    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
        this.localeProvided = true;
    }

    @Schema(maxLength = 64, example = "Africa/Cairo")
    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
        this.timezoneProvided = true;
    }

    @Schema(minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
        this.versionProvided = true;
    }

    public boolean displayNameProvided() {
        return displayNameProvided;
    }

    public boolean bioProvided() {
        return bioProvided;
    }

    public boolean avatarUrlProvided() {
        return avatarUrlProvided;
    }

    public boolean emailProvided() {
        return emailProvided;
    }

    public boolean localeProvided() {
        return localeProvided;
    }

    public boolean timezoneProvided() {
        return timezoneProvided;
    }

    public boolean versionProvided() {
        return versionProvided;
    }

    public boolean anyMutableFieldProvided() {
        return displayNameProvided
                || bioProvided
                || avatarUrlProvided
                || emailProvided
                || localeProvided
                || timezoneProvided;
    }
}
