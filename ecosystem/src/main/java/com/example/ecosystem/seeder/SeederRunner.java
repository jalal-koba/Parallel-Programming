package com.example.ecosystem.seeder;

import com.example.ecosystem.Entity.Order;
import com.example.ecosystem.Entity.Product;
import com.example.ecosystem.Entity.User;
import com.example.ecosystem.repository.OrderRepository;
import com.example.ecosystem.repository.ProductRepository;
import com.example.ecosystem.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SeederRunner implements CommandLineRunner {

    private final UserSeeder userSeeder;
    private final ProductSeeder productSeeder;
    private final OrderSeeder orderSeeder;

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public SeederRunner(
            UserSeeder userSeeder,
            ProductSeeder productSeeder,
            OrderSeeder orderSeeder,
            UserRepository userRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository
    ) {
        this.userSeeder = userSeeder;
        this.productSeeder = productSeeder;
        this.orderSeeder = orderSeeder;
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

        System.out.println("=================================");
        System.out.println("STARTING NORMAL SEED");
        System.out.println("=================================");

        int USER_COUNT = 3000;
        int PRODUCT_COUNT = 800;
        int ORDER_COUNT = 15000;

        // =========================
        // USERS
        // =========================
        List<User> users = userSeeder.generateUsers(USER_COUNT);
        userRepository.saveAll(users);
        System.out.println("✅ Users seeded: " + users.size());

        // =========================
        // PRODUCTS
        // =========================
        List<Product> products = productSeeder.generateProducts(PRODUCT_COUNT);
        productRepository.saveAll(products);
        System.out.println("✅ Products seeded: " + products.size());

        // =========================
        // ORDERS
        // =========================
        List<Order> orders = orderSeeder.generateOrders(ORDER_COUNT, users, products);
        orderRepository.saveAll(orders);
        System.out.println("✅ Orders seeded: " + orders.size());

        System.out.println("=================================");
        System.out.println("SEEDING COMPLETED SUCCESSFULLY");
        System.out.println("=================================");
    }
}