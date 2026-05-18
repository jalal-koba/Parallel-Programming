package com.example.ecosystem.batch;

import com.example.ecosystem.Entity.DailySalesSummary;
import com.example.ecosystem.Entity.Order;
import com.example.ecosystem.Entity.OrderItem;
import com.example.ecosystem.Entity.Product;
import com.example.ecosystem.Entity.Role;
import com.example.ecosystem.Entity.User;
import com.example.ecosystem.repository.DailySalesSummaryRepository;
import com.example.ecosystem.repository.OrderItemRepository;
import com.example.ecosystem.repository.OrderRepository;
import com.example.ecosystem.repository.ProductRepository;
import com.example.ecosystem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DailySalesBatchJobTest {

    @Autowired private JobLauncher jobLauncher;
    @Autowired private Job dailySalesJob;
    @Autowired private DailySalesSummaryRepository summaryRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        cleanDb();
    }

    @Test
    void jobMatchesSequentialReferenceAndIsIdempotent() throws Exception {
        LocalDate reportDate = LocalDate.of(2026, 5, 11);
        LocalDateTime inRange = reportDate.atTime(10, 0);
        LocalDateTime outOfRange = reportDate.minusDays(1).atTime(10, 0);

        User user = createUser("batch-user");
        Product laptop = createProduct("Laptop", 1000F);
        Product mouse = createProduct("Mouse", 50F);

        saveOrder(user, "COMPLETED", inRange, List.of(
                orderItem(laptop, 2, 1000F),
                orderItem(mouse, 1, 50F)
        ));
        saveOrder(user, "COMPLETED", inRange.plusHours(1), List.of(
                orderItem(mouse, 3, 50F)
        ));
        saveOrder(user, "CANCELLED", inRange, List.of(orderItem(laptop, 1, 1000F)));
        saveOrder(user, "COMPLETED", outOfRange, List.of(orderItem(laptop, 1, 1000F)));

        Map<Long, ExpectedAgg> expected = sequentialReference(reportDate);

        BatchRunReport firstReport = launch(reportDate);
        assertThat(firstReport.status()).isEqualTo(BatchStatus.COMPLETED.name());
        assertThat(firstReport.failures()).isZero();
        assertSummaryMatchesExpected(summaryRepository.findByReportDate(reportDate), expected);

        BatchRunReport secondReport = launch(reportDate);
        assertThat(secondReport.status()).isEqualTo(BatchStatus.COMPLETED.name());
        assertSummaryMatchesExpected(summaryRepository.findByReportDate(reportDate), expected);
    }

    @Test
    void jobSkipsPoisonOrderAndKeepsValidOrders() throws Exception {
        LocalDate reportDate = LocalDate.of(2026, 5, 11);
        LocalDateTime inRange = reportDate.atTime(9, 0);

        User user = createUser("poison-user");
        Product product = createProduct("Keyboard", 200F);

        saveOrder(user, "COMPLETED", inRange, List.of(orderItem(product, 2, 200F)));

        Order poisonOrder = new Order();
        poisonOrder.setUser(user);
        poisonOrder.setStatus("COMPLETED");
        poisonOrder.setCreatedAt(inRange.plusMinutes(5));
        poisonOrder.setTotalAmount(300F);

        OrderItem broken = new OrderItem();
        broken.setOrder(poisonOrder);
        broken.setProduct(null);
        broken.setQuantity(3);
        broken.setUnitPriceAtPurchase(100F);
        poisonOrder.setItems(List.of(broken));
        orderRepository.save(poisonOrder);

        BatchRunReport report = launch(reportDate);
        List<DailySalesSummary> rows = summaryRepository.findByReportDate(reportDate);

        assertThat(report.status()).isEqualTo(BatchStatus.COMPLETED.name());
        assertThat(rows).hasSize(1);
        DailySalesSummary row = rows.get(0);
        assertThat(row.getTotalQuantitySold()).isEqualTo(2L);
        assertThat(row.getTotalRevenue()).isEqualByComparingTo(BigDecimal.valueOf(400L));
    }

    private BatchRunReport launch(LocalDate reportDate) throws Exception {
        JobExecution execution = jobLauncher.run(dailySalesJob, new JobParametersBuilder()
                .addString("reportDate", reportDate.toString())
                .addLong("run.id", System.nanoTime())
                .toJobParameters());
        return BatchRunReport.from(execution);
    }

    private void cleanDb() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.execute("TRUNCATE TABLE daily_sales_summary");
        jdbcTemplate.execute("TRUNCATE TABLE order_items");
        jdbcTemplate.execute("TRUNCATE TABLE orders");
        jdbcTemplate.execute("TRUNCATE TABLE cart_items");
        jdbcTemplate.execute("TRUNCATE TABLE carts");
        jdbcTemplate.execute("TRUNCATE TABLE wishlists");
        jdbcTemplate.execute("TRUNCATE TABLE products");
        jdbcTemplate.execute("TRUNCATE TABLE users");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    private Map<Long, ExpectedAgg> sequentialReference(LocalDate reportDate) {
        LocalDateTime start = reportDate.atStartOfDay();
        LocalDateTime end = reportDate.plusDays(1).atStartOfDay();
        List<Order> all = orderRepository.findAll();
        Map<Long, ExpectedAgg> map = new HashMap<>();
        for (Order order : all) {
            if (!"COMPLETED".equals(order.getStatus()) || order.getCreatedAt().isBefore(start) || !order.getCreatedAt().isBefore(end)) {
                continue;
            }
            List<Long> counted = new ArrayList<>();
            for (OrderItem item : orderItemRepository.findByOrderId(order.getId())) {
                if (item.getProduct() == null) continue;
                Long productId = item.getProduct().getId();
                ExpectedAgg agg = map.computeIfAbsent(productId, id -> new ExpectedAgg(item.getProduct().getName()));
                agg.totalQuantity += item.getQuantity();
                agg.totalRevenue = agg.totalRevenue.add(BigDecimal.valueOf(item.getUnitPriceAtPurchase()).multiply(BigDecimal.valueOf(item.getQuantity())));
                if (!counted.contains(productId)) {
                    agg.ordersCount++;
                    counted.add(productId);
                }
            }
        }
        return map;
    }

    private void assertSummaryMatchesExpected(List<DailySalesSummary> rows, Map<Long, ExpectedAgg> expected) {
        List<DailySalesSummary> sorted = rows.stream()
                .sorted(Comparator.comparing(DailySalesSummary::getProductId))
                .toList();
        assertThat(sorted).hasSize(expected.size());
        for (DailySalesSummary row : sorted) {
            ExpectedAgg agg = expected.get(row.getProductId());
            assertThat(agg).isNotNull();
            assertThat(row.getProductName()).isEqualTo(agg.productName);
            assertThat(row.getTotalQuantitySold()).isEqualTo(agg.totalQuantity);
            assertThat(row.getOrdersCount()).isEqualTo(agg.ordersCount);
            assertThat(row.getTotalRevenue()).isEqualByComparingTo(agg.totalRevenue);
        }
    }

    private User createUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword("password");
        user.setRole(Role.customer);
        return userRepository.save(user);
    }

    private Product createProduct(String name, float price) {
        Product product = new Product();
        product.setName(name);
        product.setDescription("test");
        product.setPrice(price);
        product.setStockQuantity(1000);
        return productRepository.save(product);
    }

    private Order saveOrder(User user, String status, LocalDateTime createdAt, List<OrderItem> items) {
        Order order = new Order();
        order.setUser(user);
        order.setStatus(status);
        order.setCreatedAt(createdAt);
        float total = 0F;
        for (OrderItem item : items) {
            item.setOrder(order);
            total += item.getQuantity() * item.getUnitPriceAtPurchase();
        }
        order.setItems(items);
        order.setTotalAmount(total);
        return orderRepository.save(order);
    }

    private OrderItem orderItem(Product product, int quantity, float unitPrice) {
        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setUnitPriceAtPurchase(unitPrice);
        return item;
    }

    private static class ExpectedAgg {
        private final String productName;
        private long totalQuantity;
        private BigDecimal totalRevenue = BigDecimal.ZERO;
        private long ordersCount;

        private ExpectedAgg(String productName) {
            this.productName = productName;
        }
    }
}
