package com.example.ecosystem.seeder;

import com.example.ecosystem.Entity.Category;
import com.example.ecosystem.Entity.Product;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class ProductSeeder {

    private final Faker faker = new Faker(new Locale("en"));

    public List<Product> generateProducts(int count, List<Category> categories) {
        List<Product> products = new ArrayList<>(count);
        Random random = new Random();

        for (int i = 0; i < count; i++) {
            Product product = new Product();
            product.setName(faker.commerce().productName());
            product.setDescription(faker.lorem().paragraph());
            product.setPrice((float) faker.number().randomDouble(2, 10, 500));
            product.setStockQuantity(ThreadLocalRandom.current().nextInt(100, 5000));
            product.setCategory(categories.get(random.nextInt(categories.size())));
            products.add(product);

            if (i % 1000 == 0 && i > 0) {
                System.out.println("Generated products: " + i);
            }
        }

        return products;
    }
}
