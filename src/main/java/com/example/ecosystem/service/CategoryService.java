package com.example.ecosystem.service;

import com.example.ecosystem.Entity.Category;
import com.example.ecosystem.dto.CategoryRequest;
import com.example.ecosystem.repository.CategoryRepository;
import com.example.ecosystem.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    public Category createCategory(CategoryRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Category already exists: " + request.name());
        }
        return categoryRepository.save(new Category(request.name()));
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
    }

    public Category updateCategory(Long categoryId, CategoryRequest request) {
        Category category = getCategory(categoryId);
        categoryRepository.findByName(request.name())
                .filter(existing -> !existing.getId().equals(categoryId))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("Category already exists: " + request.name());
                });
        category.setName(request.name());
        return categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        Category category = getCategory(categoryId);
        productRepository.findByCategoryId(categoryId).forEach(product -> product.setCategory(null));
        categoryRepository.delete(category);
    }
}
