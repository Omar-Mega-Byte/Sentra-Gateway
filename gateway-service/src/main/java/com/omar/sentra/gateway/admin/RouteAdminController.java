package com.omar.sentra.gateway.admin;

import com.omar.sentra.gateway.audit.AuditService;
import com.omar.sentra.gateway.common.request.RequestAttributes;
import com.omar.sentra.gateway.routing.GatewayRoute;
import com.omar.sentra.gateway.routing.RouteRequest;
import com.omar.sentra.gateway.routing.RouteService;
import com.omar.sentra.gateway.routing.RouteValidationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Dynamic route administration API.
 */
@RestController
@RequestMapping("/api/v1/admin/routes")
@Tag(name = "Routes", description = "Validate and manage dynamic gateway routes")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "basicAuth")
public class RouteAdminController {
    private final RouteService routes;
    private final AuditService audit;

    public RouteAdminController(RouteService routes, AuditService audit) {
        this.routes = routes;
        this.audit = audit;
    }

    @GetMapping
    @Operation(summary = "List routes")
    public Flux<GatewayRoute> list() {
        return routes.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a route")
    public Mono<GatewayRoute> get(@PathVariable String id) {
        return routes.find(id);
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate a route without saving it")
    public RouteValidationResult validate(@Valid @RequestBody RouteRequest request) {
        return routes.validate(request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a route")
    public Mono<GatewayRoute> create(
            @Valid @RequestBody RouteRequest request, Principal principal, ServerWebExchange exchange) {
        return routes.create(request).delayUntil(route -> action(
                principal, "CREATE_ROUTE", route.id(), "Created dynamic route", exchange));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a route using optimistic versioning")
    public Mono<GatewayRoute> update(
            @PathVariable String id,
            @Valid @RequestBody RouteRequest request,
            Principal principal,
            ServerWebExchange exchange) {
        return routes.update(id, request).delayUntil(route ->
                action(principal, "UPDATE_ROUTE", route.id(), "Updated dynamic route", exchange));
    }

    @PostMapping("/{id}/enable")
    @Operation(summary = "Enable a route")
    public Mono<GatewayRoute> enable(
            @PathVariable String id, Principal principal, ServerWebExchange exchange) {
        return routes.setEnabled(id, true).delayUntil(route ->
                action(principal, "ENABLE_ROUTE", route.id(), "Enabled dynamic route", exchange));
    }

    @PostMapping("/{id}/disable")
    @Operation(summary = "Disable a route")
    public Mono<GatewayRoute> disable(
            @PathVariable String id, Principal principal, ServerWebExchange exchange) {
        return routes.setEnabled(id, false).delayUntil(route ->
                action(principal, "DISABLE_ROUTE", route.id(), "Disabled dynamic route", exchange));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a route")
    public Mono<Void> delete(@PathVariable String id, Principal principal, ServerWebExchange exchange) {
        return routes.delete(id).then(action(principal, "DELETE_ROUTE", id, "Deleted dynamic route", exchange));
    }

    @GetMapping("/generation")
    @Operation(summary = "Get this instance's route generation")
    public Map<String, Long> generation() {
        return Map.of("generation", routes.generation());
    }

    private Mono<Void> action(
            Principal principal, String action, String targetId, String summary, ServerWebExchange exchange) {
        return audit.adminAction(
                principal.getName(),
                action,
                "GATEWAY_ROUTE",
                targetId,
                summary,
                exchange.getAttributeOrDefault(RequestAttributes.REQUEST_ID, "unknown"));
    }
}
