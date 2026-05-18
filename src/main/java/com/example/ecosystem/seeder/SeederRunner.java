package com.example.ecosystem.seeder;

import com.example.ecosystem.Entity.Category;
import com.example.ecosystem.Entity.Order;
import com.example.ecosystem.Entity.Product;
import com.example.ecosystem.Entity.User;
import com.example.ecosystem.repository.CategoryRepository;
import com.example.ecosystem.repository.OrderRepository;
import com.example.ecosystem.repository.ProductRepository;
import com.example.ecosystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class SeederRunner implements CommandLineRunner {

    private static final int BATCH_SIZE = 2_000;

    private final CategorySeeder categorySeeder;
    private final UserSeeder userSeeder;
    private final ProductSeeder productSeeder;
    private final OrderSeeder orderSeeder;

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    @Value("${seed.users:5000}")
    private int userCount;

    @Value("${seed.products:1000}")
    private int productCount;

    @Value("${seed.orders:500000}")
    private int orderCount;

    public SeederRunner(
            CategorySeeder categorySeeder,
            UserSeeder userSeeder,
            ProductSeeder productSeeder,
            OrderSeeder orderSeeder,
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository
    ) {
        this.categorySeeder = categorySeeder;
        this.userSeeder = userSeeder;
        this.productSeeder = productSeeder;
        this.orderSeeder = orderSeeder;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public void run(String... args) {
        System.out.println("=================================");
        System.out.println("SEED CHECK START");
        System.out.println("=================================");

        if (userRepository.count() > 0) {
            System.out.println("✅ Database already seeded. Skipping...");
            return;
        }

        System.out.println("Seeding: " + userCount + " users / " + productCount + " products / " + orderCount + " orders");
        long start = System.currentTimeMillis();

        // Categories
        List<Category> categories = categoryRepository.saveAll(categorySeeder.generateCategories());
        System.out.println("✅ Categories seeded: " + categories.size());

        // Users (batched)
        List<User> allUsers = userSeeder.generateUsers(userCount);
        saveInBatches(allUsers, userRepository::saveAll);
        System.out.println("✅ Users seeded: " + allUsers.size());

        // Products (batched, with real category references)
        List<Product> allProducts = productSeeder.generateProducts(productCount, categories);
        saveInBatches(allProducts, productRepository::saveAll);
        System.out.println("✅ Products seeded: " + allProducts.size());

        // Orders (batched)
        List<Order> allOrders = orderSeeder.generateOrders(orderCount, allUsers, allProducts);
        saveInBatches(allOrders, orderRepository::saveAll);
        System.out.println("✅ Orders seeded: " + allOrders.size());

        long elapsed = System.currentTimeMillis() - start;
        System.out.println("=================================");
        System.out.println("SEEDING COMPLETED in " + elapsed + " ms");
        System.out.println("=================================");
    }

    private <T> void saveInBatches(List<T> items, java.util.function.Consumer<List<T>> saver) {
        for (int i = 0; i < items.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, items.size());
            saver.accept(new ArrayList<>(items.subList(i, end)));
            System.out.println("  saved " + end + " / " + items.size());
        }
    }
}
