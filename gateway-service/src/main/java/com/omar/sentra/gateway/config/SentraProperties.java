package com.omar.sentra.gateway.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Type-safe gateway configuration.
 */
@Validated
@ConfigurationProperties(prefix = "sentra")
public class SentraProperties {

    @NotBlank
    private String environment = "local";
    @NotBlank
    private String instanceId = "unknown";
    @Valid
    private Security security = new Security();
    @Valid
    private Routing routing = new Routing();
    @Valid
    private Limits limits = new Limits();
    @Valid
    private Audit audit = new Audit();

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public Routing getRouting() {
        return routing;
    }

    public void setRouting(Routing routing) {
        this.routing = routing;
    }

    public Limits getLimits() {
        return limits;
    }

    public void setLimits(Limits limits) {
        this.limits = limits;
    }

    public Audit getAudit() {
        return audit;
    }

    public void setAudit(Audit audit) {
        this.audit = audit;
    }

    /**
     * Authentication and request-trust settings.
     */
    public static class Security {
        private boolean localAuthEnabled;
        @Size(min = 32)
        private String apiKeyPepper = "";
        private List<String> trustedProxyCidrs = List.of();
        @Min(32)
        private int requestIdMaxLength = 128;
        @Valid
        private Signing signing = new Signing();
        private Map<String, LocalUser> localUsers = new LinkedHashMap<>();

        public boolean isLocalAuthEnabled() {
            return localAuthEnabled;
        }

        public void setLocalAuthEnabled(boolean localAuthEnabled) {
            this.localAuthEnabled = localAuthEnabled;
        }

        public String getApiKeyPepper() {
            return apiKeyPepper;
        }

        public void setApiKeyPepper(String apiKeyPepper) {
            this.apiKeyPepper = apiKeyPepper;
        }

        public List<String> getTrustedProxyCidrs() {
            return trustedProxyCidrs;
        }

        public void setTrustedProxyCidrs(List<String> trustedProxyCidrs) {
            this.trustedProxyCidrs = trustedProxyCidrs;
        }

        public int getRequestIdMaxLength() {
            return requestIdMaxLength;
        }

        public void setRequestIdMaxLength(int requestIdMaxLength) {
            this.requestIdMaxLength = requestIdMaxLength;
        }

        public Signing getSigning() {
            return signing;
        }

        public void setSigning(Signing signing) {
            this.signing = signing;
        }

        public Map<String, LocalUser> getLocalUsers() {
            return localUsers;
        }

        public void setLocalUsers(Map<String, LocalUser> localUsers) {
            this.localUsers = localUsers;
        }
    }

    /**
     * HMAC signing and replay settings.
     */
    public static class Signing {
        @NotNull
        private Duration timestampSkew = Duration.ofMinutes(5);
        @NotNull
        private Duration nonceTtl = Duration.ofMinutes(10);

        public Duration getTimestampSkew() {
            return timestampSkew;
        }

        public void setTimestampSkew(Duration timestampSkew) {
            this.timestampSkew = timestampSkew;
        }

        public Duration getNonceTtl() {
            return nonceTtl;
        }

        public void setNonceTtl(Duration nonceTtl) {
            this.nonceTtl = nonceTtl;
        }
    }

    /**
     * Local-only basic-auth account.
     */
    public static class LocalUser {
        @NotBlank
        private String username;
        @NotBlank
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    /**
     * Dynamic route validation and refresh settings.
     */
    public static class Routing {
        private List<String> allowedSchemes = List.of("http", "https");
        private List<String> allowedServiceHosts = List.of();
        @NotNull
        private Duration refreshInterval = Duration.ofSeconds(30);

        public List<String> getAllowedSchemes() {
            return allowedSchemes;
        }

        public void setAllowedSchemes(List<String> allowedSchemes) {
            this.allowedSchemes = allowedSchemes;
        }

        public List<String> getAllowedServiceHosts() {
            return allowedServiceHosts;
        }

        public void setAllowedServiceHosts(List<String> allowedServiceHosts) {
            this.allowedServiceHosts = allowedServiceHosts;
        }

        public Duration getRefreshInterval() {
            return refreshInterval;
        }

        public void setRefreshInterval(Duration refreshInterval) {
            this.refreshInterval = refreshInterval;
        }
    }

    /**
     * Request size ceilings.
     */
    public static class Limits {
        @Min(1024)
        private int maxRequestHeaderBytes = 32768;
        @Min(1024)
        private int maxRequestBodyBytes = 10485760;
        @Min(1024)
        private int maxSignedBodyBytes = 1048576;

        public int getMaxRequestHeaderBytes() {
            return maxRequestHeaderBytes;
        }

        public void setMaxRequestHeaderBytes(int maxRequestHeaderBytes) {
            this.maxRequestHeaderBytes = maxRequestHeaderBytes;
        }

        public int getMaxRequestBodyBytes() {
            return maxRequestBodyBytes;
        }

        public void setMaxRequestBodyBytes(int maxRequestBodyBytes) {
            this.maxRequestBodyBytes = maxRequestBodyBytes;
        }

        public int getMaxSignedBodyBytes() {
            return maxSignedBodyBytes;
        }

        public void setMaxSignedBodyBytes(int maxSignedBodyBytes) {
            this.maxSignedBodyBytes = maxSignedBodyBytes;
        }
    }

    /**
     * Audit query controls.
     */
    public static class Audit {
        @NotNull
        private Duration searchMaxRange = Duration.ofDays(31);

        public Duration getSearchMaxRange() {
            return searchMaxRange;
        }

        public void setSearchMaxRange(Duration searchMaxRange) {
            this.searchMaxRange = searchMaxRange;
        }
    }
}
