package com.example.ecosystem.controller;

import com.example.ecosystem.Entity.Cart;
import com.example.ecosystem.Entity.CartItem;
import com.example.ecosystem.dto.AddCartItemRequest;
import com.example.ecosystem.service.CartService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    public Cart getCart(@PathVariable Long userId) {
        return cartService.getOrCreateCart(userId);
    }

    @PostMapping("/items")
    public CartItem addProduct(@PathVariable Long userId, @Valid @RequestBody AddCartItemRequest request) {
        return cartService.addProduct(userId, request.productId(), request.quantity());
    }
}
