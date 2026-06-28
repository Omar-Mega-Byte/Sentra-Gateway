package com.omar.sentra.order.common.request;

import static com.omar.sentra.order.common.error.ServiceErrors.actorNotAllowed;
import static com.omar.sentra.order.common.error.ServiceErrors.roleRequired;
import static com.omar.sentra.order.common.error.ServiceErrors.routeNotAllowed;
import static com.omar.sentra.order.common.error.ServiceErrors.scopeRequired;
import static com.omar.sentra.order.common.error.ServiceErrors.trustedContextRequired;

import com.omar.sentra.order.config.OrderServiceProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Validates gateway provenance and resolves trusted order ownership context.
 */
@Component
public class TrustedContextResolver {
    public static final String LIST_ROUTE = "orders-list";
    public static final String GET_ROUTE = "orders-get";
    public static final String CREATE_ROUTE = "orders-create";
    public static final String CANCEL_ROUTE = "orders-cancel";
    public static final String ADMIN_LIST_ROUTE = "admin-orders-list";
    public static final String ADMIN_UPDATE_ROUTE = "admin-orders-update";

    private static final Pattern ACTOR_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]{0,63}");
    private static final Pattern ROUTE_PATTERN = Pattern.compile("[a-z0-9][a-z0-9._-]{0,127}");

    private final OrderServiceProperties properties;
    private final Set<String> allowedRouteIds;
    private final NetworkMatcher peerMatcher;

    public TrustedContextResolver(OrderServiceProperties properties) {
        this.properties = properties;
        allowedRouteIds = Set.copyOf(properties.gateway().allowedRouteIds());
        peerMatcher = new NetworkMatcher(properties.gateway().allowedPeers());
    }

    /**
     * Resolves a trusted user operation requiring one scope.
     *
     * @param request servlet request
     * @param expectedRouteId exact route identity
     * @param expectedScope required scope
     * @return validated context
     */
    public TrustedRequestContext requireUser(
            HttpServletRequest request,
            String expectedRouteId,
            String expectedScope) {
        CommonContext common = common(request, expectedRouteId);
        List<String> scopes = decodedOptionalList(request, TrustedHeaders.SCOPES);
        List<String> roles = decodedOptionalList(request, TrustedHeaders.ROLES);
        if (!scopes.contains(expectedScope)) {
            throw scopeRequired();
        }
        return common.toRequestContext(scopes, roles);
    }

    /**
     * Resolves a trusted administrator operation requiring ORDER_ADMIN.
     *
     * @param request servlet request
     * @return validated context
     */
    public TrustedRequestContext requireAdmin(HttpServletRequest request) {
        return requireAdmin(request, ADMIN_LIST_ROUTE);
    }

    /**
     * Resolves a trusted administrator operation requiring ORDER_ADMIN.
     *
     * @param request servlet request
     * @param expectedRouteId exact route identity
     * @return validated context
     */
    public TrustedRequestContext requireAdmin(HttpServletRequest request, String expectedRouteId) {
        CommonContext common = common(request, expectedRouteId);
        List<String> scopes = decodedOptionalList(request, TrustedHeaders.SCOPES);
        List<String> roles = decodedOptionalList(request, TrustedHeaders.ROLES);
        if (!roles.contains("ORDER_ADMIN")) {
            throw roleRequired();
        }
        return common.toRequestContext(scopes, roles);
    }

    private CommonContext common(HttpServletRequest request, String expectedRouteId) {
        validatePeer(request);
        String requestId = requiredVisible(
                request,
                TrustedHeaders.REQUEST_ID,
                properties.gateway().requestIdMaxLength(),
                false);
        String subject = requiredVisible(
                request,
                TrustedHeaders.SUBJECT,
                properties.gateway().subjectMaxLength(),
                true);
        String actor = requiredVisible(
                request,
                TrustedHeaders.ACTOR_TYPE,
                properties.gateway().trustedHeaderMaxLength(),
                false);
        if (!ACTOR_PATTERN.matcher(actor).matches()) {
            throw trustedContextRequired();
        }
        if (!"USER".equals(actor)) {
            throw actorNotAllowed();
        }
        List<String> clientIds = headerValues(request, TrustedHeaders.CLIENT_ID);
        if (clientIds.size() > 1) {
            throw trustedContextRequired();
        }
        if (!clientIds.isEmpty()) {
            throw actorNotAllowed();
        }
        String tenant = optionalVisible(
                request,
                TrustedHeaders.TENANT_ID,
                properties.gateway().tenantIdMaxLength(),
                true);
        String routeId = requiredVisible(
                request,
                TrustedHeaders.ROUTE_ID,
                128,
                false);
        if (!ROUTE_PATTERN.matcher(routeId).matches()) {
            throw trustedContextRequired();
        }
        if (!allowedRouteIds.contains(routeId) || !expectedRouteId.equals(routeId)) {
            throw routeNotAllowed();
        }
        request.setAttribute(RequestAttributes.ROUTE_ID, routeId);
        validateSourceIp(request);
        validateAuthTime(request);
        return new CommonContext(requestId, subject, tenant, actor, routeId);
    }

    private void validatePeer(HttpServletRequest request) {
        if (peerMatcher.configured()) {
            if (!peerMatcher.matches(request.getRemoteAddr())) {
                throw trustedContextRequired();
            }
            return;
        }
        if (properties.gateway().contextRequired() && !isLoopback(request.getRemoteAddr())) {
            throw trustedContextRequired();
        }
    }

    private List<String> decodedOptionalList(HttpServletRequest request, String name) {
        String value = optionalVisible(
                request,
                name,
                properties.gateway().trustedHeaderMaxLength(),
                true);
        if (value == null) {
            return List.of();
        }
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
        if (!value.contains(":") && !value.matches("\\d{1,3}(?:\\.\\d{1,3}){3}")) {
            throw trustedContextRequired();
        }
        try {
            InetAddress.getByName(value);
        } catch (UnknownHostException exception) {
            throw trustedContextRequired();
        }
    }

    private void validateAuthTime(HttpServletRequest request) {
        String value = optionalVisible(
                request,
                TrustedHeaders.AUTH_TIME,
                properties.gateway().trustedHeaderMaxLength(),
                false);
        if (value != null) {
            try {
                Instant.parse(value);
            } catch (DateTimeParseException exception) {
                throw trustedContextRequired();
            }
        }
    }

    private String requiredVisible(
            HttpServletRequest request,
            String name,
            int maximumLength,
            boolean spacesAllowed) {
        List<String> values = headerValues(request, name);
        if (values.size() != 1 || !valid(values.getFirst(), maximumLength, spacesAllowed)) {
            throw trustedContextRequired();
        }
        return values.getFirst();
    }

    private String optionalVisible(
            HttpServletRequest request,
            String name,
            int maximumLength,
            boolean spacesAllowed) {
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

    private record CommonContext(
            String requestId,
            String subject,
            String tenantId,
            String actorType,
            String routeId) {

        TrustedRequestContext toRequestContext(List<String> scopes, List<String> roles) {
            return new TrustedRequestContext(
                    requestId,
                    subject,
                    tenantId,
                    actorType,
                    scopes,
                    roles,
                    routeId);
        }
    }
}
