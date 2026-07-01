package com.omar.sentra.payment.web;

import static com.omar.sentra.payment.common.error.ServiceErrors.requestInvalid;

import com.omar.sentra.payment.common.error.ApiError;
import com.omar.sentra.payment.common.error.ErrorDetail;
import com.omar.sentra.payment.common.request.RequestAttributes;
import com.omar.sentra.payment.common.request.TrustedHeaders;
import com.omar.sentra.payment.idempotency.IdempotentResult;
import com.omar.sentra.payment.payment.Payment;
import com.omar.sentra.payment.payment.PaymentService;
import com.omar.sentra.payment.payment.Refund;
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
 * Internal payment and refund endpoints called by the trusted gateway.
 */
@RestController
@RequestMapping(produces = "application/json;charset=UTF-8")
@Tag(name = "Partner Payments", description = "Trusted API-client payment and refund operations")
public class PaymentController {
    private final PaymentService paymentService;
    private final PaymentResponseMapper mapper;

    public PaymentController(PaymentService paymentService, PaymentResponseMapper mapper) {
        this.paymentService = paymentService;
        this.mapper = mapper;
    }

    /**
     * Gets one payment only when it belongs to the trusted client.
     */
    @GetMapping("/internal/v1/payments/{id}")
    @Operation(
            operationId = "getPartnerPayment",
            summary = "Get a trusted-client payment",
            description = """
                    Returns one payment only when it belongs to the trusted X-Sentra-Client-Id.
                    API-key validation, optional route signing policy, and replay checks are performed by Sentra Gateway.
                    Direct client access to this internal path is unsupported.
                    """)
    @Parameters({
        @Parameter(name = TrustedHeaders.REQUEST_ID, in = ParameterIn.HEADER, required = true,
                schema = @Schema(type = "string", minLength = 1, maxLength = 128),
                example = "8e3a95b8-6674-423e-83e6-0df84c2d66d0"),
        @Parameter(name = TrustedHeaders.ACTOR_TYPE, in = ParameterIn.HEADER, required = true,
                schema = @Schema(type = "string", allowableValues = "API_CLIENT"), example = "API_CLIENT"),
        @Parameter(name = TrustedHeaders.CLIENT_ID, in = ParameterIn.HEADER, required = true,
                schema = @Schema(type = "string", minLength = 1, maxLength = 120), example = "partner-acme"),
        @Parameter(name = TrustedHeaders.KEY_ID, in = ParameterIn.HEADER, required = true,
                schema = @Schema(type = "string", minLength = 1, maxLength = 120), example = "key-acme-active"),
        @Parameter(name = TrustedHeaders.SCOPES, in = ParameterIn.HEADER, required = true,
                schema = @Schema(type = "string"), example = "payments:read"),
        @Parameter(name = TrustedHeaders.ROUTE_ID, in = ParameterIn.HEADER, required = true,
                schema = @Schema(type = "string", allowableValues = "partner-payment-read"),
                example = "partner-payment-read"),
        @Parameter(name = TrustedHeaders.SOURCE_IP, in = ParameterIn.HEADER,
                schema = @Schema(type = "string"), example = "203.0.113.10")
    })
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Owned payment",
                headers = @Header(name = "X-Request-Id", required = true, schema = @Schema(type = "string")),
                content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Payment ID is not a canonical UUID",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Missing or malformed trusted context",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Actor, route, scope, or signature evidence denied",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Unknown or foreign-client payment",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "503", description = "Payment repository unavailable",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected internal failure",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PaymentResponse> get(
            @Parameter(description = "Canonical payment UUID", required = true,
                    schema = @Schema(type = "string", format = "uuid",
                            pattern = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"),
                    example = "40000000-0000-4000-8000-000000000001")
                    @PathVariable String id,
            HttpServletRequest request) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(mapper.payment(paymentService.getPayment(
                        RequestAttributes.trustedContext(request),
                        canonicalUuid(id))));
    }

    /**
     * Creates one deterministic mock payment for the trusted client.
     */
    @PostMapping(path = "/internal/v1/payments", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "createPartnerPayment",
            summary = "Create a trusted-client payment",
            description = """
                    Creates a deterministic mock payment for the trusted X-Sentra-Client-Id.
                    Sentra Gateway validates the external API key, HMAC request signature, timestamp, and replay nonce.
                    The service requires successful gateway signature evidence and an Idempotency-Key.
                    Direct client access to this internal path is unsupported.
                    """)
    @Parameters({
        @Parameter(name = TrustedHeaders.REQUEST_ID, in = ParameterIn.HEADER, required = true,
                schema = @Schema(type = "string", minLength = 1, maxLength = 128),
                example = "8e3a95b8-6674-423e-83e6-0df84c2d66d0"),
        @Parameter(name = TrustedHeaders.ACTOR_TYPE, in = ParameterIn.HEADER, required = true,
                schema = @Schema(type = "string", allowableValues = "API_CLIENT"), example = "API_CLIENT"),
        @Parameter(name = TrustedHeaders.CLIENT_ID, in = ParameterIn.HEADER, required = true,
                schema = @Schema(type = "string", minLength = 1, maxLength = 120), example = "partner-acme"),
        @Parameter(name = TrustedHeaders.KEY_ID, in = ParameterIn.HEADER, required = true,
                schema = @Schema(type = "string", minLength = 1, maxLength = 120), example = "key-acme-active"),
        @Parameter(name = TrustedHeaders.SCOPES, in = ParameterIn.HEADER, required = true,
                schema = @Schema(type = "string"), example = "payments:write"),
        @Parameter(name = TrustedHeaders.ROUTE_ID, in = ParameterIn.HEADER, required = true,
                schema = @Schema(type = "string", allowableValues = "partner-payment-create"),
                example = "partner-payment-create"),
        @Parameter(name = TrustedHeaders.SIGNATURE_VERIFIED, in = ParameterIn.HEADER, required = true,
                schema = @Schema(type = "boolean", allowableValues = "true"), example = "true"),
        @Parameter(name = TrustedHeaders.SIGNATURE_KEY_ID, in = ParameterIn.HEADER, required = true,
                schema = @Schema(type = "string", minLength = 1, maxLength = 120), example = "sig-key-acme-active"),
        @Parameter(name = TrustedHeaders.NONCE_STATUS, in = ParameterIn.HEADER, required = true,
                schema = @Schema(type = "string", allowableValues = "accepted"), example = "accepted"),
        @Parameter(name = TrustedHeaders.SOURCE_IP, in = ParameterIn.HEADER,
                schema = @Schema(type = "string"), example = "203.0.113.10")
    })
    @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true,
            description = "Visible ASCII mutation key scoped to route and trusted client",
            schema = @Schema(type = "string", minLength = 1, maxLength = 128, pattern = "^[!-~]+$"),
            example = "postman-payment-create-001")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created payment or original idempotent result",
                headers = {
                    @Header(name = "Location", required = true,
                            schema = @Schema(type = "string", example = "/internal/v1/payments/40000000-0000-4000-8000-000000000010")),
                    @Header(name = "Cache-Control", required = true,
                            schema = @Schema(type = "string", allowableValues = "no-store")),
                    @Header(name = "Idempotency-Replayed", required = true,
                            schema = @Schema(type = "boolean")),
                    @Header(name = "X-Request-Id", required = true,
                            schema = @Schema(type = "string"))
                },
                content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid body or idempotency key",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Missing or malformed trusted context",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Actor, route, scope, or signature evidence denied",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Idempotency or merchant-reference conflict",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "413", description = "Body exceeds configured limit",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "415", description = "Content-Type is not application/json",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "503", description = "Repository unavailable or idempotency capacity exhausted",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected internal failure",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PaymentResponse> createPayment(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Payment fields only; server-controlled and security fields are rejected",
                    content = @Content(
                            schema = @Schema(implementation = CreatePaymentRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "merchantReference": "acme-order-1002",
                                      "amount": "125.50",
                                      "currency": "USD",
                                      "description": "Security gateway lab payment"
                                    }
                                    """)))
                    @RequestBody CreatePaymentRequest body,
            HttpServletRequest request) {
        IdempotentResult<Payment> result = paymentService.createPayment(
                RequestAttributes.trustedContext(request),
                body,
                request);
        return created(result.location(), result.replayed()).body(mapper.payment(result.body()));
    }

    /**
     * Creates one deterministic mock refund for an owned captured payment.
     */
    @PostMapping(path = "/internal/v1/refunds", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "createPartnerRefund",
            summary = "Create a trusted-client refund",
            description = """
                    Creates a deterministic mock refund for an owned captured payment.
                    Sentra Gateway validates the external API key, HMAC request signature, timestamp, and replay nonce.
                    The service requires successful gateway signature evidence and an Idempotency-Key.
                    Direct client access to this internal path is unsupported.
                    """)
    @Parameters({
        @Parameter(name = TrustedHeaders.REQUEST_ID, in = ParameterIn.HEADER, required = true,
                schema = @Schema(type = "string", minLength = 1, maxLength = 128),
                example = "8e3a95b8-6674-423e-83e6-0df84c2d66d0"),
        @Parameter(name = TrustedHeaders.ACTOR_TYPE, in = ParameterIn.HEADER, required = true,
                schema = @Schema(type = "string", allowableValues = "API_CLIENT"), example = "API_CLIENT"),
        @Parameter(name = TrustedHeaders.CLIENT_ID, in = ParameterIn.HEADER, required = true,
                schema = @Schema(type = "string", minLength = 1, maxLength = 120), example = "partner-acme"),
        @Parameter(name = TrustedHeaders.KEY_ID, in = ParameterIn.HEADER, required = true,
                schema = @Schema(type = "string", minLength = 1, maxLength = 120), example = "key-acme-active"),
        @Parameter(name = TrustedHeaders.SCOPES, in = ParameterIn.HEADER, required = true,
                schema = @Schema(type = "string"), example = "refunds:write"),
        @Parameter(name = TrustedHeaders.ROUTE_ID, in = ParameterIn.HEADER, required = true,
                schema = @Schema(type = "string", allowableValues = "partner-refund-create"),
                example = "partner-refund-create"),
        @Parameter(name = TrustedHeaders.SIGNATURE_VERIFIED, in = ParameterIn.HEADER, required = true,
                schema = @Schema(type = "boolean", allowableValues = "true"), example = "true"),
        @Parameter(name = TrustedHeaders.SIGNATURE_KEY_ID, in = ParameterIn.HEADER, required = true,
                schema = @Schema(type = "string", minLength = 1, maxLength = 120), example = "sig-key-acme-active"),
        @Parameter(name = TrustedHeaders.NONCE_STATUS, in = ParameterIn.HEADER, required = true,
                schema = @Schema(type = "string", allowableValues = "accepted"), example = "accepted"),
        @Parameter(name = TrustedHeaders.SOURCE_IP, in = ParameterIn.HEADER,
                schema = @Schema(type = "string"), example = "203.0.113.10")
    })
    @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true,
            description = "Visible ASCII mutation key scoped to route and trusted client",
            schema = @Schema(type = "string", minLength = 1, maxLength = 128, pattern = "^[!-~]+$"),
            example = "postman-refund-create-001")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created refund or original idempotent result",
                headers = {
                    @Header(name = "Location", required = true,
                            schema = @Schema(type = "string", example = "/internal/v1/refunds/60000000-0000-4000-8000-000000000010")),
                    @Header(name = "Cache-Control", required = true,
                            schema = @Schema(type = "string", allowableValues = "no-store")),
                    @Header(name = "Idempotency-Replayed", required = true,
                            schema = @Schema(type = "boolean")),
                    @Header(name = "X-Request-Id", required = true,
                            schema = @Schema(type = "string"))
                },
                content = @Content(schema = @Schema(implementation = RefundResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid body or idempotency key",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Missing or malformed trusted context",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Actor, route, scope, or signature evidence denied",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Unknown or foreign-client payment",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Refund, reference, or idempotency conflict",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "413", description = "Body exceeds configured limit",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "415", description = "Content-Type is not application/json",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "503", description = "Repository unavailable or idempotency capacity exhausted",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected internal failure",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<RefundResponse> createRefund(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Refund fields only; currency and server-controlled fields are rejected",
                    content = @Content(
                            schema = @Schema(implementation = CreateRefundRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "paymentId": "40000000-0000-4000-8000-000000000001",
                                      "merchantReference": "acme-refund-1002",
                                      "amount": "25.00"
                                    }
                                    """)))
                    @RequestBody CreateRefundRequest body,
            HttpServletRequest request) {
        IdempotentResult<Refund> result = paymentService.createRefund(
                RequestAttributes.trustedContext(request),
                body,
                request);
        return created(result.location(), result.replayed()).body(mapper.refund(result.body()));
    }

    private static ResponseEntity.BodyBuilder created(String location, boolean replayed) {
        return ResponseEntity.created(URI.create(location))
                .cacheControl(CacheControl.noStore())
                .header("Idempotency-Replayed", Boolean.toString(replayed));
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
                    "Payment ID must be a canonical UUID.")));
        }
    }

}
