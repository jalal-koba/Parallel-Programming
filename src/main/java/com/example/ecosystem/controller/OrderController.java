package com.example.ecosystem.controller;

import com.example.ecosystem.dto.OrderResponse;
import com.example.ecosystem.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    public OrderResponse checkout(@PathVariable Long userId) {
        return orderService.checkout(userId);
    }

    @GetMapping
    public List<OrderResponse> getOrders(@PathVariable Long userId) {
        return orderService.getOrdersForUser(userId);
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@PathVariable Long userId, @PathVariable Long orderId) {
        return orderService.getOrderForUser(userId, orderId);
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancelOrder(@PathVariable Long userId, @PathVariable Long orderId) {
        return orderService.cancelOrder(userId, orderId);
    }
}
