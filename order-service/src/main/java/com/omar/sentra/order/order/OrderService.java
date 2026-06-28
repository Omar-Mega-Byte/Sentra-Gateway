package com.omar.sentra.order.order;

import static com.omar.sentra.order.common.error.ServiceErrors.idempotencyKeyInvalid;
import static com.omar.sentra.order.common.error.ServiceErrors.orderNotFound;
import static com.omar.sentra.order.common.error.ServiceErrors.stateConflict;
import static com.omar.sentra.order.common.error.ServiceErrors.versionConflict;

import com.omar.sentra.order.common.request.TrustedContextResolver;
import com.omar.sentra.order.common.request.TrustedRequestContext;
import com.omar.sentra.order.config.OrderServiceProperties;
import com.omar.sentra.order.observability.OrderMetrics;
import com.omar.sentra.order.web.CreateOrderRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Implements ownership-safe reads, validation, and atomic order creation.
 */
@Service
public class OrderService {
    private static final String NO_TENANT_PARTITION = "<no-tenant>";

    private final OrderRepository repository;
    private final OrderValidator validator;
    private final OrderServiceProperties properties;
    private final OrderMetrics metrics;
    private final Clock clock;

    public OrderService(
            OrderRepository repository,
            OrderValidator validator,
            OrderServiceProperties properties,
            OrderMetrics metrics,
            Clock clock) {
        this.repository = repository;
        this.validator = validator;
        this.properties = properties;
        this.metrics = metrics;
        this.clock = clock;
    }

    /**
     * Lists only orders owned by the trusted tenant and subject.
     */
    public OrderPage<Order> listOwned(
            TrustedRequestContext context,
            OrderStatus status,
            int page,
            int size) {
        return repository.findOwned(context.tenantId(), context.subject(), status, page, size);
    }

    /**
     * Gets one order through an ownership-inclusive lookup.
     */
    public Order getOwned(TrustedRequestContext context, UUID id) {
        return repository.findOwnedById(id, context.tenantId(), context.subject())
                .orElseThrow(() -> {
                    metrics.repository("find-owned-by-id", "denied");
                    return orderNotFound();
                });
    }

    /**
     * Lists bounded administrator-visible orders.
     */
    public OrderPage<Order> listAdmin(
            OrderStatus status,
            String tenantId,
            String subject,
            int page,
            int size) {
        return repository.findAdmin(status, tenantId, subject, page, size);
    }

    /**
     * Creates or replays one order using the trusted owner context.
     */
    public CreateOrderResult create(
            TrustedRequestContext context,
            CreateOrderRequest request,
            HttpServletRequest servletRequest) {
        ValidatedCreateOrder validated = validator.validate(request);
        String key = idempotencyKey(servletRequest);
        Instant now = Instant.now(clock);
        Order candidate = new Order(
                UUID.randomUUID(),
                context.subject(),
                context.tenantId(),
                validated.items(),
                OrderStatus.CREATED,
                PaymentStatus.PENDING,
                FulfillmentStatus.UNFULFILLED,
                1,
                now,
                now);
        IdempotencyRequest idempotency = key == null
                ? null
                : new IdempotencyRequest(
                        TrustedContextResolver.CREATE_ROUTE,
                        context.tenantId() == null ? NO_TENANT_PARTITION : context.tenantId(),
                        context.subject(),
                        key,
                        validated.fingerprint());
        CreateOrderResult result = repository.create(candidate, idempotency, now);
        metrics.creation(result.replayed() ? "replayed" : "created");
        return result;
    }

    /**
     * Cancels an owned order when it is still mutable.
     */
    public Order cancelOwned(
            TrustedRequestContext context,
            UUID id,
            long expectedVersion) {
        Order current = getOwned(context, id);
        if (current.status() == OrderStatus.COMPLETED || current.status() == OrderStatus.CANCELLED) {
            throw stateConflict("Only created or processing orders can be cancelled.");
        }
        return repository.cancelOwned(
                        id,
                        context.tenantId(),
                        context.subject(),
                        expectedVersion,
                        Instant.now(clock))
                .orElseThrow(() -> current.version() == expectedVersion ? orderNotFound() : versionConflict());
    }

    /**
     * Updates administrator-controlled order lifecycle fields.
     */
    public Order updateAdmin(
            UUID id,
            OrderLifecycleRequest request) {
        if (request.version() < 1
                || (request.status() == null
                        && request.paymentStatus() == null
                        && request.fulfillmentStatus() == null)) {
            throw com.omar.sentra.order.common.error.ServiceErrors.requestInvalid(List.of(
                    new com.omar.sentra.order.common.error.ErrorDetail(
                            "order",
                            "invalid",
                            "A positive version and at least one lifecycle field are required.")));
        }
        return repository.updateAdmin(
                        id,
                        request.version(),
                        request.status(),
                        request.paymentStatus(),
                        request.fulfillmentStatus(),
                        Instant.now(clock))
                .orElseThrow(() -> orderNotFound());
    }

    private String idempotencyKey(HttpServletRequest request) {
        List<String> values = Collections.list(request.getHeaders("Idempotency-Key"));
        if (values.isEmpty()) {
            return null;
        }
        if (values.size() != 1) {
            throw idempotencyKeyInvalid();
        }
        String key = values.getFirst();
        if (key.isEmpty()
                || key.length() > properties.idempotency().keyMaxLength()
                || key.chars().anyMatch(character -> character < 0x21 || character > 0x7E)) {
            throw idempotencyKeyInvalid();
        }
        return key;
    }
}
