package com.omar.sentra.order.web;

import com.omar.sentra.order.order.Order;
import com.omar.sentra.order.order.OrderItem;
import com.omar.sentra.order.order.OrderPage;
import org.springframework.stereotype.Component;

/**
 * Maps internal aggregates to user-safe and administrator DTOs.
 */
@Component
public class OrderResponseMapper {

    public UserOrderResponse user(Order order) {
        return new UserOrderResponse(
                order.id(),
                order.items().stream().map(OrderResponseMapper::item).toList(),
                order.status(),
                order.paymentStatus(),
                order.fulfillmentStatus(),
                order.version(),
                order.createdAt(),
                order.updatedAt());
    }

    public AdminOrderResponse admin(Order order) {
        return new AdminOrderResponse(
                order.id(),
                order.ownerSubject(),
                order.tenantId(),
                order.items().stream().map(OrderResponseMapper::item).toList(),
                order.status(),
                order.paymentStatus(),
                order.fulfillmentStatus(),
                order.version(),
                order.createdAt(),
                order.updatedAt());
    }

    public UserOrderPageResponse userPage(OrderPage<Order> page) {
        return new UserOrderPageResponse(
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages(),
                page.items().stream().map(this::user).toList());
    }

    public AdminOrderPageResponse adminPage(OrderPage<Order> page) {
        return new AdminOrderPageResponse(
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages(),
                page.items().stream().map(this::admin).toList());
    }

    private static OrderItemResponse item(OrderItem item) {
        return new OrderItemResponse(item.sku(), item.quantity());
    }
}
