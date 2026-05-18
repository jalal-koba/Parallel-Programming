package com.example.ecosystem.nfr;

import com.example.ecosystem.Entity.Product;
import com.example.ecosystem.Entity.Role;
import com.example.ecosystem.Entity.User;
import com.example.ecosystem.repository.OrderRepository;
import com.example.ecosystem.repository.ProductRepository;
import com.example.ecosystem.repository.UserRepository;
import com.example.ecosystem.service.CartService;
import com.example.ecosystem.service.OrderService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NFR #3 — {@code @Async} + {@code ThreadPoolTaskExecutor} + transactional events.
 * Listener sleeps 5s; checkout must return much sooner (non-blocking request thread).
 */
@SpringBootTest
@Tag("nfr3")
class Nfr3AsyncProcessingTests {

  private static final long LISTENER_SLEEP_MS = 5_000L;
  private static final long MAX_CHECKOUT_RESPONSE_MS = 3_000L;

  @Autowired
  @Qualifier("taskExecutor")
  private Executor taskExecutor;

  @Autowired
  private OrderService orderService;

  @Autowired
  private CartService cartService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private OrderRepository orderRepository;

  @Test
  void taskExecutorBeanUsesConfiguredThreadPool() {
    assertThat(taskExecutor).isInstanceOf(ThreadPoolTaskExecutor.class);

    ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) taskExecutor;
    assertThat(pool.getCorePoolSize()).isEqualTo(4);
    assertThat(pool.getMaxPoolSize()).isEqualTo(8);
    assertThat(pool.getQueueCapacity()).isEqualTo(100);
    assertThat(pool.getThreadNamePrefix()).isEqualTo("Async-Executor-");
  }

  @Test
  void checkoutReturnsBeforeSlowAsyncListenerFinishes() throws Exception {
    User user = new User();
    user.setUsername("async-buyer");
    user.setEmail("async-buyer@example.com");
    user.setPassword("password");
    user.setRole(Role.customer);
    user = userRepository.save(user);

    Product product = new Product();
    product.setName("Async Widget");
    product.setDescription("async test");
    product.setPrice(50F);
    product.setStockQuantity(10);
    product = productRepository.save(product);

    cartService.addProduct(user.getId(), product.getId(), 1);

    long ordersBefore = orderRepository.count();
    long start = System.currentTimeMillis();
    orderService.checkout(user.getId());
    long elapsed = System.currentTimeMillis() - start;

    assertThat(elapsed).isLessThan(MAX_CHECKOUT_RESPONSE_MS);
    assertThat(elapsed).isLessThan(LISTENER_SLEEP_MS);
    assertThat(orderRepository.count()).isEqualTo(ordersBefore + 1);
  }
}
