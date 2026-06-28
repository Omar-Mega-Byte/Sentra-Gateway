package com.omar.sentra.order.order;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Fixed local and test dataset covering ownership and tenant isolation.
 */
public final class OrderSeedData {
    public static final UUID OWNED_COMPLETED_ID =
            UUID.fromString("10000000-0000-4000-8000-000000000001");
    public static final UUID OWNED_CREATED_ID =
            UUID.fromString("10000000-0000-4000-8000-000000000002");
    public static final UUID FOREIGN_SUBJECT_ID =
            UUID.fromString("20000000-0000-4000-8000-000000000001");
    public static final UUID FOREIGN_TENANT_ID =
            UUID.fromString("30000000-0000-4000-8000-000000000001");
    public static final String DEMO_SUBJECT = "sentra-user-omar";
    public static final String DEMO_TENANT = "tenant-demo";

    private OrderSeedData() {}

    /**
     * Returns a fresh immutable deterministic dataset.
     *
     * @return sample orders
     */
    public static List<Order> orders() {
        return List.of(
                new Order(
                        OWNED_COMPLETED_ID,
                        DEMO_SUBJECT,
                        DEMO_TENANT,
                        List.of(new OrderItem("BOOK-JAVA-25", 1)),
                        OrderStatus.COMPLETED,
                        PaymentStatus.PAID,
                        FulfillmentStatus.DELIVERED,
                        2,
                        Instant.parse("2026-06-01T10:00:00Z"),
                        Instant.parse("2026-06-02T09:30:00Z")),
                new Order(
                        OWNED_CREATED_ID,
                        DEMO_SUBJECT,
                        DEMO_TENANT,
                        List.of(new OrderItem("SECURE-GATEWAY-LAB", 1)),
                        OrderStatus.CREATED,
                        PaymentStatus.PENDING,
                        FulfillmentStatus.UNFULFILLED,
                        1,
                        Instant.parse("2026-06-10T14:00:00Z"),
                        Instant.parse("2026-06-10T14:00:00Z")),
                new Order(
                        FOREIGN_SUBJECT_ID,
                        "sentra-user-other",
                        DEMO_TENANT,
                        List.of(new OrderItem("FOREIGN-SUBJECT-SAMPLE", 2)),
                        OrderStatus.PROCESSING,
                        PaymentStatus.PAID,
                        FulfillmentStatus.PROCESSING,
                        2,
                        Instant.parse("2026-06-08T11:00:00Z"),
                        Instant.parse("2026-06-09T11:00:00Z")),
                new Order(
                        FOREIGN_TENANT_ID,
                        DEMO_SUBJECT,
                        "tenant-other",
                        List.of(new OrderItem("FOREIGN-TENANT-SAMPLE", 1)),
                        OrderStatus.COMPLETED,
                        PaymentStatus.PAID,
                        FulfillmentStatus.DELIVERED,
                        2,
                        Instant.parse("2026-06-05T08:00:00Z"),
                        Instant.parse("2026-06-06T08:00:00Z")));
    }
}
