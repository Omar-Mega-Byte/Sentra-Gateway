package com.omar.sentra.payment.common.request;

import com.omar.sentra.payment.config.PaymentServiceProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Restricts detailed management endpoints to configured operations networks in
 * production-like profiles.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class ManagementAccessFilter extends OncePerRequestFilter {
    private static final Set<String> DEVELOPMENT_PROFILES = Set.of("local", "test");

    private final boolean productionLike;
    private final NetworkMatcher allowedNetworks;

    public ManagementAccessFilter(Environment environment, PaymentServiceProperties properties) {
        productionLike = Arrays.stream(environment.getActiveProfiles())
                .map(profile -> profile.toLowerCase(Locale.ROOT))
                .anyMatch(profile -> !DEVELOPMENT_PROFILES.contains(profile));
        allowedNetworks = new NetworkMatcher(properties.management().allowedCidrs());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean detailedManagement = path.startsWith("/actuator/prometheus")
                || path.startsWith("/actuator/metrics")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui");
        return !productionLike || !detailedManagement;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!allowedNetworks.configured() || !allowedNetworks.matches(request.getRemoteAddr())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
