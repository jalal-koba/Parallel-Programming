package com.example.ecosystem.service;

import com.example.ecosystem.Entity.Cart;
import com.example.ecosystem.Entity.CartItem;
import com.example.ecosystem.Entity.Order;
import com.example.ecosystem.Entity.OrderItem;
import com.example.ecosystem.Entity.Product;
import com.example.ecosystem.dto.OrderItemResponse;
import com.example.ecosystem.dto.OrderResponse;
import com.example.ecosystem.event.OrderCreatedEvent;
import com.example.ecosystem.repository.CartItemRepository;
import com.example.ecosystem.repository.CartRepository;
import com.example.ecosystem.repository.OrderRepository;
import com.example.ecosystem.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class OrderService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final org.redisson.api.RedissonClient redissonClient;
    private final OrderTransactionService orderTransactionService;

    public OrderService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            OrderRepository orderRepository,
            ProductRepository productRepository,
            ApplicationEventPublisher eventPublisher,
            org.redisson.api.RedissonClient redissonClient,
            OrderTransactionService orderTransactionService
    ) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
        this.redissonClient = redissonClient;
        this.orderTransactionService = orderTransactionService;
    }

    public OrderResponse checkout(Long userId) {
        org.redisson.api.RLock lock = redissonClient.getLock("checkout:lock:user:" + userId);
        try {
            // Using tryLock with waitTime to allow Watchdog to extend lease time automatically
            if (lock.tryLock(5, java.util.concurrent.TimeUnit.SECONDS)) {
                return orderTransactionService.executeCheckoutTransaction(userId);
            } else {
                throw new IllegalStateException("Checkout is already in progress for this user. Please try again.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Checkout interrupted", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersForUser(Long userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(this::toOrderResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderForUser(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        return toOrderResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if ("CANCELLED".equals(order.getStatus())) {
            throw new IllegalStateException("Order is already cancelled");
        }

        for (OrderItem orderItem : order.getItems()) {
            Product product = productRepository.findByIdWithPessimisticLock(orderItem.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + orderItem.getProduct().getId()));
            product.setStockQuantity(product.getStockQuantity() + orderItem.getQuantity());
        }

        order.setStatus("CANCELLED");
        return toOrderResponse(orderRepository.save(order));
    }

    private OrderResponse toOrderResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUser().getId(),
                order.getCreatedAt(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getItems().stream().map(this::toOrderItemResponse).toList()
        );
    }

    private OrderItemResponse toOrderItemResponse(OrderItem orderItem) {
        Product product = orderItem.getProduct();
        return new OrderItemResponse(
                orderItem.getId(),
                product.getId(),
                product.getName(),
                orderItem.getQuantity(),
                orderItem.getUnitPriceAtPurchase()
        );
    }
}
