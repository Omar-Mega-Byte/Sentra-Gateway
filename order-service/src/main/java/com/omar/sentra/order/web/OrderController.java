package com.omar.sentra.order.web;

import static com.omar.sentra.order.common.error.ServiceErrors.requestInvalid;

import com.omar.sentra.order.common.error.ApiError;
import com.omar.sentra.order.common.error.ErrorDetail;
import com.omar.sentra.order.common.request.RequestAttributes;
import com.omar.sentra.order.order.CreateOrderResult;
import com.omar.sentra.order.order.OrderVersionRequest;
import com.omar.sentra.order.order.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal current-user order endpoints called by the trusted gateway.
 */
@RestController
@RequestMapping(path = "/internal/v1/orders", produces = "application/json;charset=UTF-8")
@Tag(name = "User Orders", description = "Trusted subject-scoped and tenant-scoped order operations")
public class OrderController {
    private final OrderService orderService;
    private final OrderQueryParser queryParser;
    private final OrderResponseMapper mapper;

    public OrderController(
            OrderService orderService,
            OrderQueryParser queryParser,
            OrderResponseMapper mapper) {
        this.orderService = orderService;
        this.queryParser = queryParser;
        this.mapper = mapper;
    }

    /**
     * Lists orders belonging to the exact trusted owner key.
     */
    @GetMapping
    @Operation(
            operationId = "listCurrentUserOrders",
            summary = "List current user orders",
            description = """
                    Returns only orders owned by the exact trusted tenant and subject.
                    External JWT authentication is performed by Sentra Gateway. Direct
                    client access to this internal path is unsupported.
                    """)
    @Parameters({
        @Parameter(name = "page", in = ParameterIn.QUERY,
                schema = @Schema(type = "integer", minimum = "0", maximum = "10000", defaultValue = "0")),
        @Parameter(name = "size", in = ParameterIn.QUERY,
                schema = @Schema(type = "integer", minimum = "1", maximum = "100", defaultValue = "20")),
        @Parameter(name = "status", in = ParameterIn.QUERY,
                schema = @Schema(type = "string",
                        allowableValues = {"CREATED", "PROCESSING", "COMPLETED", "CANCELLED"}))
    })
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Owner-scoped page",
                content = @Content(schema = @Schema(implementation = UserOrderPageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid or unknown query parameter",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Missing or malformed trusted context",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Actor, route, or orders:read scope denied",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "503", description = "Order repository unavailable",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected internal failure",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<UserOrderPageResponse> list(HttpServletRequest request) {
        OrderQueryParser.Query query = queryParser.parseUser(request);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(mapper.userPage(orderService.listOwned(
                        RequestAttributes.trustedContext(request),
                        query.status(),
                        query.page(),
                        query.size())));
    }

    /**
     * Gets one order only when the exact trusted owner key matches.
     */
    @GetMapping("/{id}")
    @Operation(
            operationId = "getCurrentUserOrder",
            summary = "Get a current user order",
            description = "Unknown and foreign orders intentionally return the same 404 response.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Owned order",
                content = @Content(schema = @Schema(implementation = UserOrderResponse.class))),
        @ApiResponse(responseCode = "400", description = "Order ID is not a canonical UUID",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Missing or malformed trusted context",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Actor, route, or orders:read scope denied",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Unknown, foreign-subject, or foreign-tenant order",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "503", description = "Order repository unavailable",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected internal failure",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<UserOrderResponse> get(
            @Parameter(description = "Canonical order UUID", required = true,
                    schema = @Schema(type = "string", format = "uuid",
                            pattern = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"),
                    example = "10000000-0000-4000-8000-000000000001")
                    @PathVariable String id,
            HttpServletRequest request) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(mapper.user(orderService.getOwned(
                        RequestAttributes.trustedContext(request),
                        canonicalUuid(id))));
    }

    /**
     * Creates one order from trusted ownership and validated client items.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "createCurrentUserOrder",
            summary = "Create a current user order",
            description = """
                    Creates a CREATED order. Idempotency-Key is optional and strongly
                    recommended. Automatic upstream retries are unsupported unless the
                    same key and body are preserved.
                    """)
    @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER,
            description = "Optional visible ASCII create key scoped to route, tenant, and subject",
            schema = @Schema(type = "string", minLength = 1, maxLength = 128, pattern = "^[!-~]+$"),
            example = "postman-order-create-001")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created order or original idempotent result",
                headers = {
                    @Header(name = "Location", required = true,
                            schema = @Schema(type = "string",
                                    example = "/internal/v1/orders/10000000-0000-4000-8000-000000000001")),
                    @Header(name = "Cache-Control", required = true,
                            schema = @Schema(type = "string", allowableValues = "no-store")),
                    @Header(name = "Idempotency-Replayed",
                            description = "Present only for keyed creates",
                            schema = @Schema(type = "boolean"))
                },
                content = @Content(schema = @Schema(implementation = UserOrderResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid body or idempotency key",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Missing or malformed trusted context",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Actor, route, or orders:write scope denied",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Idempotency key payload conflict",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "413", description = "Body exceeds the configured limit",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "415", description = "Content-Type is not application/json",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "503", description = "Repository unavailable or idempotency capacity exhausted",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected internal failure",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<UserOrderResponse> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Items only; owner and server-controlled fields are rejected",
                    content = @Content(
                            schema = @Schema(implementation = CreateOrderRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "items": [
                                        {"sku": "BOOK-JAVA-25", "quantity": 1},
                                        {"sku": "SECURE-GATEWAY-LAB", "quantity": 2}
                                      ]
                                    }
                                    """)))
                    @RequestBody CreateOrderRequest body,
            HttpServletRequest request) {
        CreateOrderResult result = orderService.create(
                RequestAttributes.trustedContext(request),
                body,
                request);
        ResponseEntity.BodyBuilder response = ResponseEntity.created(
                        URI.create("/internal/v1/orders/" + result.order().id()))
                .cacheControl(CacheControl.noStore());
        if (result.keyed()) {
            response.header("Idempotency-Replayed", Boolean.toString(result.replayed()));
        }
        return response.body(mapper.user(result.order()));
    }

    /**
     * Cancels one owned order using optimistic concurrency.
     */
    @PostMapping(path = "/{id}/cancel", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "cancelCurrentUserOrder",
            summary = "Cancel a current user order",
            description = "Cancels an owned CREATED or PROCESSING order.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cancelled owned order",
                content = @Content(schema = @Schema(implementation = UserOrderResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid ID or body",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "orders:write scope denied",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Unknown or foreign order",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Version or state conflict",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<UserOrderResponse> cancel(
            @PathVariable String id,
            @RequestBody OrderVersionRequest body,
            HttpServletRequest request) {
        RequestAttributes.trustedContext(request);
        if (body.version() < 1) {
            throw requestInvalid(List.of(new ErrorDetail(
                    "version",
                    "minimum",
                    "Version must be positive.")));
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(mapper.user(orderService.cancelOwned(
                        RequestAttributes.trustedContext(request),
                        canonicalUuid(id),
                        body.version())));
    }

    private static UUID canonicalUuid(String value) {
        try {
            UUID id = UUID.fromString(value);
            if (!id.toString().equals(value)) {
                throw new IllegalArgumentException("Non-canonical UUID.");
            }
            return id;
        } catch (IllegalArgumentException exception) {
            throw requestInvalid(List.of(new ErrorDetail(
                    "id",
                    "format",
                    "Order ID must be a canonical UUID.")));
        }
    }
}
