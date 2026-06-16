package com.omar.sentra.gateway.admin;

import com.omar.sentra.gateway.audit.AuditService;
import com.omar.sentra.gateway.common.error.ErrorCode;
import com.omar.sentra.gateway.common.error.GatewayException;
import com.omar.sentra.gateway.common.request.RequestAttributes;
import com.omar.sentra.gateway.security.apikey.ApiClient;
import com.omar.sentra.gateway.security.apikey.ApiClientRepository;
import com.omar.sentra.gateway.security.apikey.ApiKeyRecord;
import com.omar.sentra.gateway.security.apikey.ApiKeyService;
import com.omar.sentra.gateway.security.apikey.ClientStatus;
import com.omar.sentra.gateway.security.apikey.IssuedApiKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * API client and one-time key lifecycle API.
 */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "API Clients", description = "Manage partner clients and API keys")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "basicAuth")
public class ApiClientAdminController {
    private final ApiClientRepository repository;
    private final ApiKeyService keys;
    private final AuditService audit;

    public ApiClientAdminController(ApiClientRepository repository, ApiKeyService keys, AuditService audit) {
        this.repository = repository;
        this.keys = keys;
        this.audit = audit;
    }

    @GetMapping("/api-clients")
    @Operation(summary = "List API clients")
    public Flux<ApiClient> clients() {
        return repository.findClients();
    }

    @GetMapping("/api-clients/{id}")
    @Operation(summary = "Get an API client")
    public Mono<ApiClient> client(@PathVariable UUID id) {
        return repository.findClient(id)
                .switchIfEmpty(Mono.error(new GatewayException(ErrorCode.GW_RESOURCE_NOT_FOUND)));
    }

    @PostMapping("/api-clients")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an API client")
    public Mono<ApiClient> create(
            @Valid @RequestBody ClientRequest request, Principal principal, ServerWebExchange exchange) {
        Instant now = Instant.now();
        ApiClient client = new ApiClient(
                UUID.randomUUID(), request.name(), request.owner(), request.tenantId(),
                ClientStatus.ACTIVE, 0, now, now);
        return repository.insertClient(client).delayUntil(saved ->
                action(principal, "CREATE_API_CLIENT", saved.id().toString(), exchange));
    }

    @PutMapping("/api-clients/{id}")
    @Operation(summary = "Update an API client")
    public Mono<ApiClient> update(
            @PathVariable UUID id,
            @Valid @RequestBody ClientUpdateRequest request,
            Principal principal,
            ServerWebExchange exchange) {
        ApiClient client = new ApiClient(
                id, request.name(), request.owner(), request.tenantId(), request.status(),
                request.version(), Instant.EPOCH, Instant.now());
        return repository.updateClient(client, request.version())
                .switchIfEmpty(Mono.error(new GatewayException(ErrorCode.GW_POLICY_CONFLICT)))
                .delayUntil(saved -> action(principal, "UPDATE_API_CLIENT", saved.id().toString(), exchange));
    }

    @PostMapping("/api-clients/{id}/disable")
    @Operation(summary = "Disable an API client")
    public Mono<ApiClient> disable(
            @PathVariable UUID id, Principal principal, ServerWebExchange exchange) {
        return client(id)
                .flatMap(existing -> repository.updateClient(new ApiClient(
                        existing.id(), existing.name(), existing.owner(), existing.tenantId(),
                        ClientStatus.DISABLED, existing.version(), existing.createdAt(), Instant.now()),
                        existing.version()))
                .switchIfEmpty(Mono.error(new GatewayException(ErrorCode.GW_POLICY_CONFLICT)))
                .delayUntil(saved -> action(principal, "DISABLE_API_CLIENT", saved.id().toString(), exchange));
    }

    @GetMapping("/api-clients/{id}/keys")
    @Operation(summary = "List API-key metadata")
    public Flux<ApiKeyMetadata> keyMetadata(@PathVariable UUID id) {
        return repository.findKeysByClient(id).map(ApiKeyMetadata::from);
    }

    @PostMapping("/api-clients/{id}/keys")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Issue an API key; plaintext is returned once")
    public Mono<IssuedApiKey> issue(
            @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody KeyRequest request,
            Principal principal,
            ServerWebExchange exchange) {
        return keys.issue(id, request.scopes(), request.allowedRoutes(), request.expiresAt(), null)
                .delayUntil(saved -> action(principal, "ISSUE_API_KEY", saved.keyId().toString(), exchange));
    }

    @PostMapping("/api-keys/{id}/rotate")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Rotate an API key")
    public Mono<IssuedApiKey> rotate(
            @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody RotationRequest request,
            Principal principal,
            ServerWebExchange exchange) {
        return keys.rotate(id, request.expiresAt())
                .delayUntil(saved -> action(principal, "ROTATE_API_KEY", saved.keyId().toString(), exchange));
    }

    @PostMapping("/api-keys/{id}/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke an API key immediately")
    public Mono<Void> revoke(@PathVariable UUID id, Principal principal, ServerWebExchange exchange) {
        return keys.revoke(id).then(action(principal, "REVOKE_API_KEY", id.toString(), exchange));
    }

    private Mono<Void> action(Principal principal, String action, String targetId, ServerWebExchange exchange) {
        return audit.adminAction(
                principal.getName(), action, "API_CLIENT_OR_KEY", targetId, action,
                exchange.getAttributeOrDefault(RequestAttributes.REQUEST_ID, "unknown"));
    }

    /**
     * Client creation body.
     */
    public record ClientRequest(
            @NotBlank @Size(max = 160) String name,
            @NotBlank @Size(max = 200) String owner,
            @Size(max = 120) String tenantId) {
    }

    /**
     * Client replacement body.
     */
    public record ClientUpdateRequest(
            @NotBlank @Size(max = 160) String name,
            @NotBlank @Size(max = 200) String owner,
            @Size(max = 120) String tenantId,
            ClientStatus status,
            long version) {
    }

    /**
     * Key issuance body.
     */
    public record KeyRequest(
            @Size(max = 100) List<@NotBlank String> scopes,
            @Size(max = 100) List<@NotBlank String> allowedRoutes,
            @Future Instant expiresAt) {
    }

    /**
     * Key rotation body.
     */
    public record RotationRequest(@Future Instant expiresAt) {
    }

    /**
     * Public API-key metadata. Cryptographic verifier material is intentionally excluded.
     */
    public record ApiKeyMetadata(
            UUID id,
            UUID clientId,
            String prefix,
            List<String> scopes,
            List<String> allowedRoutes,
            com.omar.sentra.gateway.security.apikey.KeyStatus status,
            Instant validFrom,
            Instant expiresAt,
            UUID rotatedFrom,
            Instant lastUsedAt,
            Instant createdAt) {

        static ApiKeyMetadata from(ApiKeyRecord key) {
            return new ApiKeyMetadata(
                    key.id(),
                    key.clientId(),
                    key.prefix(),
                    key.scopes(),
                    key.allowedRoutes(),
                    key.status(),
                    key.validFrom(),
                    key.expiresAt(),
                    key.rotatedFrom(),
                    key.lastUsedAt(),
                    key.createdAt());
        }
    }
}
