package com.omar.sentra.gateway.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Local basic-auth and production JWT security chains.
 */
@Configuration
public class SecurityConfig {

    @Bean
    @ConditionalOnProperty(prefix = "sentra.security", name = "local-auth-enabled", havingValue = "true")
    MapReactiveUserDetailsService localUsers(SentraProperties properties) {
        List<UserDetails> users = new ArrayList<>();
        properties.getSecurity().getLocalUsers().forEach((name, configured) -> {
            String[] roles = "operator".equals(name)
                    ? new String[] {"GATEWAY_OPERATOR", "GATEWAY_AUDITOR"}
                    : new String[] {
                        "GATEWAY_ROUTE_ADMIN",
                        "GATEWAY_SECURITY_ADMIN",
                        "GATEWAY_AUDITOR",
                        "GATEWAY_OPERATOR",
                        "GATEWAY_SUPER_ADMIN"
                    };
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            for (String role : roles) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
            authorities.add(new SimpleGrantedAuthority("SCOPE_profile:read"));
            authorities.add(new SimpleGrantedAuthority("SCOPE_profile:write"));
            authorities.add(new SimpleGrantedAuthority("SCOPE_orders:read"));
            authorities.add(new SimpleGrantedAuthority("SCOPE_orders:write"));
            users.add(User.withUsername(configured.getUsername())
                    .password("{noop}" + configured.getPassword())
                    .authorities(authorities)
                    .build());
        });
        return new MapReactiveUserDetailsService(users);
    }

    @Bean
    @ConditionalOnProperty(prefix = "sentra.security", name = "local-auth-enabled", havingValue = "true")
    SecurityWebFilterChain localSecurity(ServerHttpSecurity http) {
        return commonAuthorization(http, true)
                .httpBasic(Customizer.withDefaults())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .build();
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "sentra.security",
            name = "local-auth-enabled",
            havingValue = "false",
            matchIfMissing = true)
    SecurityWebFilterChain jwtSecurity(ServerHttpSecurity http) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setPrincipalClaimName("sub");
        converter.setJwtGrantedAuthoritiesConverter(jwtAuthorities());
        return commonAuthorization(http, false)
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(new ReactiveJwtAuthenticationConverterAdapter(converter))))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .build();
    }

    private ServerHttpSecurity commonAuthorization(ServerHttpSecurity http, boolean local) {
        return http.authorizeExchange(exchanges -> exchanges
                .pathMatchers("/actuator/health/**")
                .permitAll()
                .pathMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
                .access((authentication, context) -> local
                        ? reactor.core.publisher.Mono.just(new org.springframework.security.authorization.AuthorizationDecision(true))
                        : authentication.map(value -> new org.springframework.security.authorization.AuthorizationDecision(
                                value.getAuthorities().stream().anyMatch(authority ->
                                        authority.getAuthority().equals("ROLE_GATEWAY_OPERATOR")
                                                || authority.getAuthority().equals("ROLE_GATEWAY_SUPER_ADMIN")))))
                .pathMatchers("/actuator/prometheus", "/actuator/metrics/**", "/actuator/info")
                .hasAnyRole("GATEWAY_OPERATOR", "GATEWAY_SUPER_ADMIN")
                .pathMatchers("/api/v1/admin/audit-events/**", "/api/v1/admin/admin-actions/**")
                .hasAnyRole("GATEWAY_AUDITOR", "GATEWAY_SUPER_ADMIN")
                .pathMatchers(HttpMethod.GET, "/api/v1/admin/routes/**")
                .hasAnyRole("GATEWAY_ROUTE_ADMIN", "GATEWAY_OPERATOR", "GATEWAY_SUPER_ADMIN")
                .pathMatchers("/api/v1/admin/routes/**")
                .hasAnyRole("GATEWAY_ROUTE_ADMIN", "GATEWAY_SUPER_ADMIN")
                .pathMatchers("/api/v1/admin/**")
                .hasAnyRole("GATEWAY_SECURITY_ADMIN", "GATEWAY_SUPER_ADMIN")
                .anyExchange()
                .permitAll());
    }

    private Converter<Jwt, Collection<GrantedAuthority>> jwtAuthorities() {
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
        return jwt -> {
            Collection<GrantedAuthority> scopeAuthorities = scopes.convert(jwt);
            Stream<String> roles = Stream.concat(
                    simpleRoles(jwt.getClaims().get("roles")),
                    keycloakRoles(jwt.getClaims()));
            return Stream.concat(
                            scopeAuthorities == null ? Stream.empty() : scopeAuthorities.stream(),
                            roles.filter(value -> !value.isBlank())
                                    .map(value -> (GrantedAuthority) new SimpleGrantedAuthority(
                                            value.startsWith("ROLE_") ? value : "ROLE_" + value)))
                    .distinct()
                    .toList();
        };
    }

    private Stream<String> simpleRoles(Object claim) {
        return claim instanceof Collection<?> values
                ? values.stream().map(Object::toString)
                : claim == null ? Stream.empty() : Stream.of(claim.toString().split("[ ,]"));
    }

    @SuppressWarnings("unchecked")
    private Stream<String> keycloakRoles(Map<String, Object> claims) {
        Stream<String> realmRoles = Stream.empty();
        Object realmAccess = claims.get("realm_access");
        if (realmAccess instanceof Map<?, ?> realm && realm.get("roles") instanceof Collection<?> roles) {
            realmRoles = roles.stream().map(Object::toString);
        }
        Stream<String> resourceRoles = Stream.empty();
        Object resourceAccess = claims.get("resource_access");
        if (resourceAccess instanceof Map<?, ?> resources) {
            resourceRoles = resources.values().stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .map(resource -> resource.get("roles"))
                    .filter(Collection.class::isInstance)
                    .map(Collection.class::cast)
                    .flatMap(Collection::stream)
                    .map(Object::toString);
        }
        return Stream.concat(realmRoles, resourceRoles);
    }
}
