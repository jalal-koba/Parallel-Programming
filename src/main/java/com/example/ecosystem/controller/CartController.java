package com.example.ecosystem.controller;

import com.example.ecosystem.dto.AddCartItemRequest;
import com.example.ecosystem.dto.CartItemResponse;
import com.example.ecosystem.dto.CartResponse;
import com.example.ecosystem.dto.UpdateCartItemRequest;
import com.example.ecosystem.service.CartService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}/cart")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse getCart(@PathVariable Long userId) {
        return cartService.getOrCreateCart(userId);
    }

    @PostMapping("/items")
    public CartItemResponse addProduct(@PathVariable Long userId, @Valid @RequestBody AddCartItemRequest request) {
        return cartService.addProduct(userId, request.productId(), request.quantity());
    }

    @PutMapping("/items/{productId}")
    public CartItemResponse updateProductQuantity(
            @PathVariable Long userId,
            @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        return cartService.updateProductQuantity(userId, productId, request.quantity());
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse removeProduct(@PathVariable Long userId, @PathVariable Long productId) {
        return cartService.removeProduct(userId, productId);
    }

    @DeleteMapping("/items")
    public CartResponse clearCart(@PathVariable Long userId) {
        return cartService.clearCart(userId);
    }
}
