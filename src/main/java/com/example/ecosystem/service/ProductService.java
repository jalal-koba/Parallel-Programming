package com.example.ecosystem.service;

import com.example.ecosystem.Entity.Category;
import com.example.ecosystem.Entity.Product;
import com.example.ecosystem.dto.ProductRequest;
import com.example.ecosystem.repository.CategoryRepository;
import com.example.ecosystem.repository.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @CacheEvict(value = "productsList", allEntries = true)
    public Product createProduct(ProductRequest request) {
        Product product = new Product();
        applyProductRequest(product, request);
        return productRepository.save(product);
    }

    @Transactional
    public void createProductsTransactional(List<ProductRequest> requests) {
        for (ProductRequest request : requests) {
            createProduct(request);
        }
    }

    public void createProductsNonTransactional(List<ProductRequest> requests) {
        for (ProductRequest request : requests) {
            createProduct(request);
        }
    }


    @Cacheable(value = "productsList")
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Cacheable(value = "product", key = "#productId")
    public Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
    }

    public List<Product> getProductsByCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category not found: " + categoryId);
        }
        return productRepository.findByCategoryId(categoryId);
    }

    @CacheEvict(value = {"product", "productsList"}, key = "#productId", allEntries = false)
    public Product updateProduct(Long productId, ProductRequest request) {
        Product product = getProduct(productId);
        applyProductRequest(product, request);
        return productRepository.save(product);
    }

    @CacheEvict(value = {"product", "productsList"}, key = "#productId", allEntries = false)
    public void deleteProduct(Long productId) {
        Product product = getProduct(productId);
        productRepository.delete(product);
    }

    private void applyProductRequest(Product product, ProductRequest request) {
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());

        if (request.categoryId() == null) {
            product.setCategory(null);
            return;
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.categoryId()));
        product.setCategory(category);
    }
}
