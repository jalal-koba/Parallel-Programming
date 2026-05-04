package com.example.ecosystem;

import com.example.ecosystem.Entity.Product;
import com.example.ecosystem.Entity.Role;
import com.example.ecosystem.Entity.User;
import com.example.ecosystem.dto.ProductRequest;
import com.example.ecosystem.repository.OrderRepository;
import com.example.ecosystem.repository.ProductRepository;
import com.example.ecosystem.repository.UserRepository;
import com.example.ecosystem.service.CartService;
import com.example.ecosystem.service.InsufficientStockException;
import com.example.ecosystem.service.OrderService;
import com.example.ecosystem.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrderServiceConcurrencyTests {
    private final ProductService productService;
    private final CartService cartService;
    private final OrderService orderService;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @Autowired
    OrderServiceConcurrencyTests(
            ProductService productService,
            CartService cartService,
            OrderService orderService,
            ProductRepository productRepository,
            UserRepository userRepository,
            OrderRepository orderRepository
    ) {
        this.productService = productService;
        this.cartService = cartService;
        this.orderService = orderService;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @Test
    void checkoutDoesNotOversellWhenManyUsersBuyAtTheSameTime() throws Exception {
        Product product = productService.createProduct(
                new ProductRequest("Parallel Laptop", "Concurrency test product", 1000F, 10)
        );

        List<Long> userIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            User user = new User();
            user.setUsername("buyer-" + i);
            user.setEmail("buyer-" + i + "@example.com");
            user.setPassword("password");
            user.setRole(Role.customer);
            User savedUser = userRepository.save(user);
            userIds.add(savedUser.getId());
            cartService.addProduct(savedUser.getId(), product.getId(), 1);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(userIds.size());
        CountDownLatch ready = new CountDownLatch(userIds.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (Long userId : userIds) {
            futures.add(executorService.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    orderService.checkout(userId);
                    return true;
                } catch (InsufficientStockException exception) {
                    return false;
                }
            }));
        }

        ready.await();
        start.countDown();

        long successfulCheckouts = 0;
        for (Future<Boolean> future : futures) {
            if (future.get()) {
                successfulCheckouts++;
            }
        }
        executorService.shutdown();

        Product reloadedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(successfulCheckouts).isEqualTo(10);
        assertThat(orderRepository.count()).isEqualTo(10);
        assertThat(reloadedProduct.getStockQuantity()).isZero();
    }
}
