package com.example.ecosystem.batch;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class LargeBatchDataSeeder {

    private static final int FLUSH_THRESHOLD = 5_000;
    private static final int PROGRESS_EVERY_ORDERS = 50_000;
    private static final int STOCK_PLENTY = 1_000_000;
    private static final int MAX_QUANTITY_PER_ITEM = 4;
    private static final String ORDER_STATUS_COMPLETED = "COMPLETED";
    private static final String DEFAULT_ROLE = "customer";
    private static final String DEFAULT_PASSWORD = "password";

    private static final List<String> TABLES_TO_TRUNCATE =
            List.of("daily_sales_summary", "order_items", "orders", "products", "users");

    private static final String INSERT_USER =
            "INSERT INTO users (id, username, email, password, role) VALUES (?, ?, ?, ?, ?)";
    private static final String INSERT_PRODUCT =
            "INSERT INTO products (id, name, description, price, stock_quantity, version, category_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String INSERT_ORDER =
            "INSERT INTO orders (id, user_id, created_at, status, total_amount) VALUES (?, ?, ?, ?, ?)";
    private static final String INSERT_ORDER_ITEM =
            "INSERT INTO order_items (id, order_id, product_id, quantity, unit_price_at_purchase) VALUES (?, ?, ?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;

    public LargeBatchDataSeeder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void seedCompletedOrders(LocalDate reportDate, int orderCount, int userCount, int productCount) {
        BatchConsole.line("");
        BatchConsole.tag("SEED", "Truncating benchmark tables...");
        truncateAll();

        long usersStart = System.currentTimeMillis();
        seedUsers(userCount);
        BatchConsole.tagf("SEED", "Users: %s inserted in %s",
                BatchConsole.highlight(String.format("%,d", userCount)),
                BatchConsole.highlight(formatElapsed(usersStart)));

        long productsStart = System.currentTimeMillis();
        seedProducts(productCount);
        BatchConsole.tagf("SEED", "Products: %s inserted in %s",
                BatchConsole.highlight(String.format("%,d", productCount)),
                BatchConsole.highlight(formatElapsed(productsStart)));

        long ordersStart = System.currentTimeMillis();
        seedOrdersAndItems(reportDate, orderCount, userCount, productCount);
        BatchConsole.tagf("SEED", "Orders + items: %s each inserted in %s",
                BatchConsole.highlight(String.format("%,d", orderCount)),
                BatchConsole.highlight(formatElapsed(ordersStart)));
    }

    private void truncateAll() {
        TABLES_TO_TRUNCATE.forEach(table -> jdbcTemplate.update("DELETE FROM " + table));
    }

    private void seedUsers(int count) {
        BatchInserter inserter = new BatchInserter(INSERT_USER);
        for (long id = 1; id <= count; id++) {
            inserter.add(id, "bench-user-" + id, "bench-user-" + id + "@example.com", DEFAULT_PASSWORD, DEFAULT_ROLE);
        }
        inserter.flushRemaining();
    }

    private void seedProducts(int count) {
        BatchInserter inserter = new BatchInserter(INSERT_PRODUCT);
        for (long id = 1; id <= count; id++) {
            inserter.add(id, "Bench Product " + id, "Generated for heavy batch test",
                    priceFor(id), STOCK_PLENTY, 0, null);
        }
        inserter.flushRemaining();
    }

    private void seedOrdersAndItems(LocalDate reportDate, int orderCount, int userCount, int productCount) {
        Timestamp createdAt = Timestamp.valueOf(reportDate.atTime(12, 0));
        BatchInserter orders = new BatchInserter(INSERT_ORDER);
        BatchInserter items = new BatchInserter(INSERT_ORDER_ITEM);

        for (long i = 0; i < orderCount; i++) {
            long orderId = i + 1;
            long userId = (i % userCount) + 1;
            long productId = (i % productCount) + 1;
            int quantity = (int) (i % MAX_QUANTITY_PER_ITEM) + 1;
            float unitPrice = priceFor(productId);

            orders.add(orderId, userId, createdAt, ORDER_STATUS_COMPLETED, unitPrice * quantity);
            items.add(orderId, orderId, productId, quantity, unitPrice);

            long inserted = i + 1;
            if (inserted % PROGRESS_EVERY_ORDERS == 0 || inserted == orderCount) {
                BatchConsole.tagf("SEED", "orders/items progress: %s / %s (%s)",
                        BatchConsole.highlight(String.format("%,d", inserted)),
                        String.format("%,d", orderCount),
                        BatchConsole.highlight(String.format("%.1f%%", (inserted * 100.0) / orderCount)));
            }
        }
        orders.flushRemaining();
        items.flushRemaining();
    }

    private static float priceFor(long productId) {
        return 50F + ((productId - 1) % 100);
    }

    private static String formatElapsed(long startMs) {
        long elapsedMs = System.currentTimeMillis() - startMs;
        if (elapsedMs < 1_000) {
            return elapsedMs + " ms";
        }
        return String.format("%.1f s", elapsedMs / 1_000.0);
    }

    private final class BatchInserter {
        private final String sql;
        private final List<Object[]> buffer = new ArrayList<>(FLUSH_THRESHOLD);

        private BatchInserter(String sql) {
            this.sql = sql;
        }

        private void add(Object... values) {
            buffer.add(values);
            if (buffer.size() >= FLUSH_THRESHOLD) {
                flush();
            }
        }

        private void flushRemaining() {
            if (!buffer.isEmpty()) {
                flush();
            }
        }

        private void flush() {
            jdbcTemplate.batchUpdate(sql, buffer);
            buffer.clear();
        }
    }
}
