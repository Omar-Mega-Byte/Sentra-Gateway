package com.omar.sentra.gateway.admin;

import com.omar.sentra.gateway.audit.AdminAction;
import com.omar.sentra.gateway.audit.AuditEvent;
import com.omar.sentra.gateway.audit.AuditRepository;
import com.omar.sentra.gateway.common.error.ErrorCode;
import com.omar.sentra.gateway.common.error.GatewayException;
import com.omar.sentra.gateway.config.SentraProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Bounded audit investigation API.
 */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Audit")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "basicAuth")
public class AuditAdminController {
    private final AuditRepository repository;
    private final SentraProperties properties;

    public AuditAdminController(AuditRepository repository, SentraProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @GetMapping("/audit-events")
    @Operation(summary = "Search audit events in a bounded time range")
    public Flux<AuditEvent> search(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) String routeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        if (from.isAfter(to)
                || Duration.between(from, to).compareTo(properties.getAudit().getSearchMaxRange()) > 0
                || page < 0
                || pageSize < 1
                || pageSize > 100) {
            throw new GatewayException(ErrorCode.GW_REQUEST_INVALID);
        }
        return repository.search(from, to, requestId, routeId, pageSize, page * pageSize);
    }

    @GetMapping("/audit-events/{id}")
    public Mono<AuditEvent> get(@PathVariable UUID id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new GatewayException(ErrorCode.GW_RESOURCE_NOT_FOUND)));
    }

    @GetMapping("/admin-actions")
    public Flux<AdminAction> actions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        if (page < 0 || pageSize < 1 || pageSize > 100) {
            throw new GatewayException(ErrorCode.GW_REQUEST_INVALID);
        }
        return repository.findAdminActions(pageSize, page * pageSize);
    }
}
