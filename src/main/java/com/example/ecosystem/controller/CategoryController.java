package com.example.ecosystem.controller;

import com.example.ecosystem.Entity.Category;
import com.example.ecosystem.dto.CategoryRequest;
import com.example.ecosystem.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Category createCategory(@Valid @RequestBody CategoryRequest request) {
        return categoryService.createCategory(request);
    }

    @GetMapping
    public List<Category> getCategories() {
        return categoryService.getAllCategories();
    }

    @GetMapping("/{categoryId}")
    public Category getCategory(@PathVariable Long categoryId) {
        return categoryService.getCategory(categoryId);
    }

    @PutMapping("/{categoryId}")
    public Category updateCategory(@PathVariable Long categoryId, @Valid @RequestBody CategoryRequest request) {
        return categoryService.updateCategory(categoryId, request);
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long categoryId) {
        categoryService.deleteCategory(categoryId);
    }
}
