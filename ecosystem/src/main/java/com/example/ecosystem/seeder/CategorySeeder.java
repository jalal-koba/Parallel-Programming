package com.example.ecosystem.seeder;

import com.example.ecosystem.Entity.Category;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CategorySeeder {

    private static final List<String> NAMES = List.of(
            "Electronics",
            "Clothing",
            "Home & Kitchen",
            "Sports",
            "Books",
            "Beauty",
            "Toys",
            "Automotive"
    );

    public List<Category> generateCategories() {
        return NAMES.stream()
                .map(Category::new)
                .toList();
    }
}
