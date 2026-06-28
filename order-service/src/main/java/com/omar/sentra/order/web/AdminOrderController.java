package com.omar.sentra.order.web;

import com.omar.sentra.order.common.error.ApiError;
import com.omar.sentra.order.common.request.RequestAttributes;
import com.omar.sentra.order.common.request.TrustedContextResolver;
import com.omar.sentra.order.order.OrderLifecycleRequest;
import com.omar.sentra.order.order.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal administrator order inspection endpoint.
 */
@RestController
@RequestMapping(path = "/internal/v1/admin/orders", produces = "application/json;charset=UTF-8")
@Tag(name = "Administrator Orders", description = "Bounded ORDER_ADMIN order inspection")
public class AdminOrderController {
    private final OrderService orderService;
    private final OrderQueryParser queryParser;
    private final OrderResponseMapper mapper;
    private final TrustedContextResolver contexts;

    public AdminOrderController(
            OrderService orderService,
            OrderQueryParser queryParser,
            OrderResponseMapper mapper,
            TrustedContextResolver contexts) {
        this.orderService = orderService;
        this.queryParser = queryParser;
        this.mapper = mapper;
        this.contexts = contexts;
    }

    /**
     * Lists bounded administrator representations across owners.
     */
    @GetMapping
    @Operation(
            operationId = "listOrdersAsAdministrator",
            summary = "List orders as administrator",
            description = """
                    Requires USER actor and ORDER_ADMIN. This is an internal path;
                    external JWT authentication is performed by Sentra Gateway and
                    direct client access is unsupported.
                    """)
    @Parameters({
        @Parameter(name = "page", in = ParameterIn.QUERY,
                schema = @Schema(type = "integer", minimum = "0", maximum = "10000", defaultValue = "0")),
        @Parameter(name = "size", in = ParameterIn.QUERY,
                schema = @Schema(type = "integer", minimum = "1", maximum = "100", defaultValue = "20")),
        @Parameter(name = "status", in = ParameterIn.QUERY,
                schema = @Schema(type = "string",
                        allowableValues = {"CREATED", "PROCESSING", "COMPLETED", "CANCELLED"})),
        @Parameter(name = "tenantId", in = ParameterIn.QUERY,
                schema = @Schema(type = "string", minLength = 1, maxLength = 128)),
        @Parameter(name = "subject", in = ParameterIn.QUERY,
                schema = @Schema(type = "string", minLength = 1, maxLength = 255))
    })
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Administrator-visible page",
                content = @Content(schema = @Schema(implementation = AdminOrderPageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid or unknown query parameter",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Missing or malformed trusted context",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Actor, route, or ORDER_ADMIN role denied",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "503", description = "Order repository unavailable",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected internal failure",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<AdminOrderPageResponse> list(HttpServletRequest request) {
        RequestAttributes.trustedContext(request);
        OrderQueryParser.Query query = queryParser.parseAdmin(request);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(mapper.adminPage(orderService.listAdmin(
                        query.status(),
                        query.tenantId(),
                        query.subject(),
                        query.page(),
                        query.size())));
    }

    @PatchMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "updateOrderAsAdministrator",
            summary = "Update order lifecycle fields")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Updated administrator-visible order",
                content = @Content(schema = @Schema(implementation = AdminOrderResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid body",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "ORDER_ADMIN denied",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Order not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Version conflict",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<AdminOrderResponse> update(
            @PathVariable java.util.UUID id,
            @RequestBody OrderLifecycleRequest body,
            HttpServletRequest request) {
        contexts.requireAdmin(request, TrustedContextResolver.ADMIN_UPDATE_ROUTE);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(mapper.admin(orderService.updateAdmin(id, body)));
    }
}
