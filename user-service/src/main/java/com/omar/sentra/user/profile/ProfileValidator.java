package com.omar.sentra.user.profile;

import static com.omar.sentra.user.common.error.ServiceErrors.requestInvalid;
import static com.omar.sentra.user.common.error.ServiceErrors.versionConflict;

import com.omar.sentra.user.common.error.ErrorDetail;
import com.omar.sentra.user.config.UserServiceProperties;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Validates and normalizes merge-style profile updates.
 */
@Component
public class ProfileValidator {
    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?"
                    + "(?:\\.[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?)+$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern LANGUAGE_TAG =
            Pattern.compile("^[A-Za-z]{2,8}(?:-[A-Za-z0-9]{1,8})*$");
    private static final Set<String> METADATA_HOSTS = Set.of(
            "169.254.169.254",
            "metadata.google.internal",
            "metadata.azure.internal",
            "instance-data.ec2.internal");

    private final UserServiceProperties.Limits limits;

    public ProfileValidator(UserServiceProperties properties) {
        this.limits = properties.limits();
    }

    /**
     * Validates a request and resolves omitted fields from the current profile.
     *
     * @param request patch request
     * @param current current stored profile
     * @return normalized target values
     */
    public ValidatedProfilePatch validate(ProfilePatchRequest request, UserProfile current) {
        List<ErrorDetail> details = new ArrayList<>();
        if (!request.versionProvided() || request.getVersion() == null || request.getVersion() < 1) {
            details.add(detail("version", "required", "A positive current version is required."));
        }
        if (!request.anyMutableFieldProvided()) {
            details.add(detail("profile", "empty", "At least one mutable field is required."));
        }

        String displayName = current.displayName();
        if (request.displayNameProvided()) {
            displayName = request.getDisplayName();
            if (displayName == null) {
                details.add(detail("displayName", "not_null", "Display name cannot be null."));
            } else {
                displayName = displayName.trim();
                if (displayName.isEmpty() || displayName.length() > limits.maxDisplayNameLength()) {
                    details.add(detail(
                            "displayName",
                            "length",
                            "Display name must be within the configured length."));
                }
            }
        }

        String bio = current.bio();
        if (request.bioProvided()) {
            bio = request.getBio();
            if (bio != null && bio.length() > limits.maxBioLength()) {
                details.add(detail("bio", "length", "Biography exceeds the configured length."));
            }
        }

        String avatarUrl = current.avatarUrl();
        if (request.avatarUrlProvided()) {
            avatarUrl = request.getAvatarUrl();
            if (avatarUrl != null && !validAvatarUrl(avatarUrl)) {
                details.add(detail(
                        "avatarUrl",
                        "https_uri",
                        "Avatar URL must be an approved absolute HTTPS URI."));
            }
        }

        String email = current.email();
        if (request.emailProvided()) {
            email = request.getEmail();
            if (email == null) {
                details.add(detail("email", "not_null", "Email cannot be null."));
            } else {
                email = email.trim().toLowerCase(Locale.ROOT);
                if (email.length() > limits.maxEmailLength() || !EMAIL.matcher(email).matches()) {
                    details.add(detail("email", "format", "Email must be a valid address."));
                }
            }
        }

        String locale = current.locale();
        if (request.localeProvided()) {
            locale = request.getLocale();
            if (locale == null) {
                details.add(detail("locale", "not_null", "Locale cannot be null."));
            } else if (locale.length() > limits.maxLocaleLength()
                    || !LANGUAGE_TAG.matcher(locale).matches()) {
                details.add(detail("locale", "format", "Locale must be a valid BCP 47 tag."));
            } else {
                locale = Locale.forLanguageTag(locale).toLanguageTag();
            }
        }

        String timezone = current.timezone();
        if (request.timezoneProvided()) {
            timezone = request.getTimezone();
            if (timezone == null) {
                details.add(detail("timezone", "not_null", "Time zone cannot be null."));
            } else if (timezone.length() > limits.maxTimezoneLength()) {
                details.add(detail("timezone", "length", "Time zone exceeds the configured length."));
            } else {
                try {
                    timezone = ZoneId.of(timezone).getId();
                } catch (DateTimeException exception) {
                    details.add(detail("timezone", "format", "Time zone must be a valid IANA ID."));
                }
            }
        }

        if (!details.isEmpty()) {
            throw requestInvalid(details);
        }
        if (request.getVersion() != current.version()) {
            throw versionConflict();
        }
        return new ValidatedProfilePatch(
                displayName,
                bio,
                avatarUrl,
                email,
                locale,
                timezone,
                request.getVersion());
    }

    private boolean validAvatarUrl(String value) {
        if (value.length() > limits.maxAvatarUrlLength()) {
            return false;
        }
        try {
            URI uri = new URI(value);
            String host = uri.getHost();
            if (!uri.isAbsolute()
                    || !"https".equalsIgnoreCase(uri.getScheme())
                    || host == null
                    || uri.getUserInfo() != null
                    || uri.getFragment() != null) {
                return false;
            }
            String normalizedHost = host.toLowerCase(Locale.ROOT);
            if ("localhost".equals(normalizedHost)
                    || normalizedHost.endsWith(".localhost")
                    || METADATA_HOSTS.contains(normalizedHost)) {
                return false;
            }
            if (looksLikeIpLiteral(normalizedHost)) {
                InetAddress address = InetAddress.getByName(stripIpv6Brackets(normalizedHost));
                return !address.isLoopbackAddress() && !address.isLinkLocalAddress();
            }
            return true;
        } catch (URISyntaxException | UnknownHostException exception) {
            return false;
        }
    }

    private static boolean looksLikeIpLiteral(String host) {
        return host.contains(":") || host.matches("\\d{1,3}(?:\\.\\d{1,3}){3}");
    }

    private static String stripIpv6Brackets(String host) {
        return host.startsWith("[") && host.endsWith("]")
                ? host.substring(1, host.length() - 1)
                : host;
    }

    private static ErrorDetail detail(String field, String code, String message) {
        return new ErrorDetail(field, code, message);
    }
}
