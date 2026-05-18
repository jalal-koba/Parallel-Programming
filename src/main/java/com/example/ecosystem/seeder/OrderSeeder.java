package com.example.ecosystem.seeder;

import com.example.ecosystem.Entity.*;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class OrderSeeder {

    public List<Order> generateOrders(
            int count,
            List<User> users,
            List<Product> products
    ) {

        List<Order> orders = new ArrayList<>(count);

        Random random = new Random();

        for (int i = 0; i < count; i++) {

            User user =
                    users.get(random.nextInt(users.size()));

            Order order = new Order();

            order.setUser(user);

            order.setStatus(
                    random.nextDouble() < 0.9
                            ? "COMPLETED"
                            : "CANCELLED"
            );

            order.setCreatedAt(
                    LocalDateTime.now()
                            .minusDays(random.nextInt(365))
                            .minusHours(random.nextInt(24))
                            .minusMinutes(random.nextInt(60))
            );

            List<OrderItem> items = new ArrayList<>();

            int itemCount =
                    ThreadLocalRandom.current().nextInt(1, 6);

            float total = 0F;

            for (int j = 0; j < itemCount; j++) {

                Product product =
                        products.get(random.nextInt(products.size()));

                int quantity =
                        ThreadLocalRandom.current().nextInt(1, 5);

                OrderItem item = new OrderItem();

                item.setOrder(order);

                item.setProduct(product);

                item.setQuantity(quantity);

                item.setUnitPriceAtPurchase(product.getPrice());

                total += product.getPrice() * quantity;

                items.add(item);
            }

            order.setItems(items);

            order.setTotalAmount(total);

            orders.add(order);

            if (i % 5000 == 0) {
                System.out.println("Generated orders: " + i);
            }
        }

        return orders;
    }
}