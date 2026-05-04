package com.example.ecosystem.service;

import com.example.ecosystem.Entity.Cart;
import com.example.ecosystem.Entity.CartItem;
import com.example.ecosystem.Entity.Order;
import com.example.ecosystem.Entity.OrderItem;
import com.example.ecosystem.Entity.Product;
import com.example.ecosystem.repository.CartItemRepository;
import com.example.ecosystem.repository.CartRepository;
import com.example.ecosystem.repository.OrderRepository;
import com.example.ecosystem.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public OrderService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            OrderRepository orderRepository,
            ProductRepository productRepository
    ) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public Order checkout(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userId));

        List<CartItem> cartItems = new ArrayList<>(cart.getItems());
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Cannot checkout an empty cart");
        }

        cartItems.sort(Comparator.comparing(item -> item.getProduct().getId()));

        Order order = new Order();
        order.setUser(cart.getUser());
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus("COMPLETED");

        List<OrderItem> orderItems = new ArrayList<>();
        float totalAmount = 0F;

        for (CartItem cartItem : cartItems) {
            Product product = productRepository.findByIdWithPessimisticLock(cartItem.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + cartItem.getProduct().getId()));

            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new InsufficientStockException("Not enough stock for product: " + product.getId());
            }

            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            totalAmount += product.getPrice() * cartItem.getQuantity();

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPriceAtPurchase(product.getPrice());
            orderItems.add(orderItem);
        }

        order.setTotalAmount(totalAmount);
        order.setItems(orderItems);
        Order savedOrder = orderRepository.save(order);
        cartItemRepository.deleteByCart(cart);
        return savedOrder;
    }
}
