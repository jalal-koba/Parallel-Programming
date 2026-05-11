package com.example.ecosystem.seeder;

import com.example.ecosystem.Entity.Category;
import com.example.ecosystem.Entity.Product;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class ProductSeeder {

    private final Faker faker = new Faker(new Locale("en"));
    private final List<String> CATEGORY_NAMES = List.of(
        "Electronics",
        "Clothing",
        "Home & Kitchen",
        "Sports",
        "Books",
        "Beauty",
        "Toys",
        "Automotive"
);


    public List<Product> generateProducts(int count) {

        List<Product> products = new ArrayList<>(count);
        Random random = new Random();

        for (int i = 0; i < count; i++) {

            Product product = new Product();

            product.setName(
                    faker.commerce().productName()
            );

            product.setDescription(
                    faker.lorem().paragraph()
            );

            product.setPrice(
                    (float) faker.number().randomDouble(2, 10, 500)
            );

            product.setStockQuantity(
                    ThreadLocalRandom.current().nextInt(100, 5000)
            );

            Category category = new Category();
            category.setName(
                    CATEGORY_NAMES.get(random.nextInt(CATEGORY_NAMES.size()))
            );

            products.add(product);

            if (i % 500 == 0) {
                System.out.println("Generated products: " + i);
            }
        }

        return products;
    }
}