package com.omar.sentra.payment.common.request;

import static com.omar.sentra.payment.common.error.ServiceErrors.actorNotAllowed;
import static com.omar.sentra.payment.common.error.ServiceErrors.routeNotAllowed;
import static com.omar.sentra.payment.common.error.ServiceErrors.scopeRequired;
import static com.omar.sentra.payment.common.error.ServiceErrors.signatureContextRequired;
import static com.omar.sentra.payment.common.error.ServiceErrors.trustedContextRequired;

import com.omar.sentra.payment.config.PaymentServiceProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Validates gateway provenance and resolves trusted payment client context.
 */
@Component
public class TrustedContextResolver {
    public static final String PAYMENT_READ_ROUTE = "partner-payment-read";
    public static final String PAYMENT_CREATE_ROUTE = "partner-payment-create";
    public static final String REFUND_CREATE_ROUTE = "partner-refund-create";

    private static final Set<String> DEVELOPMENT_PROFILES = Set.of("local", "test");
    private static final Pattern ACTOR_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]{0,63}");
    private static final Pattern ROUTE_PATTERN = Pattern.compile("[a-z0-9][a-z0-9._-]{0,127}");
    private static final Set<String> RAW_CREDENTIAL_HEADERS = Set.of(
            "authorization",
            "api-key",
            "x-api-key",
            "x-signature",
            "signature",
            "x-hmac-signature",
            "x-api-signature",
            "x-request-signature",
            "x-sentra-signature");

    private final PaymentServiceProperties properties;
    private final Set<String> allowedRouteIds;
    private final NetworkMatcher peerMatcher;
    private final boolean productionLike;

    public TrustedContextResolver(Environment environment, PaymentServiceProperties properties) {
        this.properties = properties;
        allowedRouteIds = Set.copyOf(properties.gateway().allowedRouteIds());
        peerMatcher = new NetworkMatcher(properties.gateway().allowedPeers());
        productionLike = Arrays.stream(environment.getActiveProfiles())
                .map(profile -> profile.toLowerCase(Locale.ROOT))
                .anyMatch(profile -> !DEVELOPMENT_PROFILES.contains(profile));
    }

    /**
     * Resolves a trusted partner operation requiring one scope.
     *
     * @param request servlet request
     * @param expectedRouteId exact route identity
     * @param expectedScope required scope
     * @param signatureRequired whether gateway signature evidence is required
     * @return validated context
     */
    public TrustedRequestContext requirePartner(
            HttpServletRequest request,
            String expectedRouteId,
            String expectedScope,
            boolean signatureRequired) {
        rejectRawCredentials(request);
        validatePeer(request);
        String requestId = requiredVisible(request, TrustedHeaders.REQUEST_ID, properties.gateway().requestIdMaxLength(), false);
        String actor = requiredVisible(request, TrustedHeaders.ACTOR_TYPE, properties.gateway().trustedHeaderMaxLength(), false);
        if (!ACTOR_PATTERN.matcher(actor).matches()) {
            throw trustedContextRequired();
        }
        if (!"API_CLIENT".equals(actor)) {
            throw actorNotAllowed();
        }
        String clientId = requiredVisible(request, TrustedHeaders.CLIENT_ID, properties.gateway().clientIdMaxLength(), false);
        String keyId = requiredVisible(request, TrustedHeaders.KEY_ID, properties.gateway().keyIdMaxLength(), false);
        String routeId = requiredVisible(request, TrustedHeaders.ROUTE_ID, 128, false);
        if (!ROUTE_PATTERN.matcher(routeId).matches()) {
            throw trustedContextRequired();
        }
        if (!allowedRouteIds.contains(routeId) || !expectedRouteId.equals(routeId)) {
            throw routeNotAllowed();
        }
        request.setAttribute(RequestAttributes.ROUTE_ID, routeId);
        List<String> scopes = decodedRequiredList(request, TrustedHeaders.SCOPES);
        if (!scopes.contains(expectedScope)) {
            throw scopeRequired();
        }
        validateSourceIp(request);
        if (signatureRequired) {
            validateSignatureEvidence(request);
        }
        return new TrustedRequestContext(requestId, actor, clientId, keyId, scopes, routeId);
    }

    private void validateSignatureEvidence(HttpServletRequest request) {
        String verified = requiredSignatureVisible(
                request,
                properties.signature().verifiedHeader(),
                properties.gateway().trustedHeaderMaxLength(),
                false);
        if (!"true".equalsIgnoreCase(verified)) {
            throw signatureContextRequired();
        }
        requiredSignatureVisible(
                request,
                properties.signature().keyIdHeader(),
                properties.gateway().keyIdMaxLength(),
                false);
        String nonceStatus = requiredSignatureVisible(
                request,
                properties.signature().nonceStatusHeader(),
                properties.gateway().trustedHeaderMaxLength(),
                false);
        if (!properties.signature().nonceAcceptedValue().equalsIgnoreCase(nonceStatus)) {
            throw signatureContextRequired();
        }
    }

    private String requiredSignatureVisible(
            HttpServletRequest request,
            String name,
            int maximumLength,
            boolean spacesAllowed) {
        List<String> values = headerValues(request, name);
        if (values.size() != 1 || !valid(values.getFirst(), maximumLength, spacesAllowed)) {
            throw signatureContextRequired();
        }
        return values.getFirst();
    }

    private void rejectRawCredentials(HttpServletRequest request) {
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            if (RAW_CREDENTIAL_HEADERS.contains(names.nextElement().toLowerCase(Locale.ROOT))) {
                throw trustedContextRequired();
            }
        }
    }

    private void validatePeer(HttpServletRequest request) {
        if (peerMatcher.configured()) {
            if (peerMatcher.matches(request.getRemoteAddr())) {
                return;
            }
            throw trustedContextRequired();
        }
        if (productionLike) {
            throw trustedContextRequired();
        }
    }

    private List<String> decodedRequiredList(HttpServletRequest request, String name) {
        String value = requiredVisible(request, name, properties.gateway().trustedHeaderMaxLength(), true);
        try {
            return EscapedListCodec.decode(value);
        } catch (IllegalArgumentException exception) {
            throw trustedContextRequired();
        }
    }

    private void validateSourceIp(HttpServletRequest request) {
        String value = optionalVisible(request, TrustedHeaders.SOURCE_IP, 64, false);
        if (value == null) {
            return;
        }
        try {
            InetAddress.getByName(value);
        } catch (UnknownHostException exception) {
            throw trustedContextRequired();
        }
    }

    private String requiredVisible(HttpServletRequest request, String name, int maximumLength, boolean spacesAllowed) {
        List<String> values = headerValues(request, name);
        if (values.size() != 1 || !valid(values.getFirst(), maximumLength, spacesAllowed)) {
            throw trustedContextRequired();
        }
        return values.getFirst();
    }

    private String optionalVisible(HttpServletRequest request, String name, int maximumLength, boolean spacesAllowed) {
        List<String> values = headerValues(request, name);
        if (values.size() > 1) {
            throw trustedContextRequired();
        }
        if (values.isEmpty()) {
            return null;
        }
        if (!valid(values.getFirst(), maximumLength, spacesAllowed)) {
            throw trustedContextRequired();
        }
        return values.getFirst();
    }

    private static boolean valid(String value, int maximumLength, boolean spacesAllowed) {
        if (value == null || value.isBlank() || value.length() > maximumLength) {
            return false;
        }
        int minimum = spacesAllowed ? 0x20 : 0x21;
        return value.chars().allMatch(character -> character >= minimum && character <= 0x7E);
    }

    private static List<String> headerValues(HttpServletRequest request, String name) {
        return Collections.list(request.getHeaders(name));
    }

    private static boolean isLoopback(String value) {
        try {
            return InetAddress.getByName(value).isLoopbackAddress();
        } catch (UnknownHostException exception) {
            return false;
        }
    }
}
