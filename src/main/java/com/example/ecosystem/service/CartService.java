package com.example.ecosystem.service;

import com.example.ecosystem.Entity.Cart;
import com.example.ecosystem.Entity.CartItem;
import com.example.ecosystem.Entity.Product;
import com.example.ecosystem.Entity.User;
import com.example.ecosystem.dto.CartItemResponse;
import com.example.ecosystem.dto.CartResponse;
import com.example.ecosystem.repository.CartItemRepository;
import com.example.ecosystem.repository.CartRepository;
import com.example.ecosystem.repository.ProductRepository;
import com.example.ecosystem.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ProductRepository productRepository,
            UserRepository userRepository
    ) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CartResponse getOrCreateCart(Long userId) {
        return toCartResponse(getOrCreateCartEntity(userId));
    }

    @Transactional
    public CartItemResponse addProduct(Long userId, Long productId, Integer quantity) {
        Cart cart = getOrCreateCartEntity(userId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        CartItem cartItem = cartItemRepository.findByCartAndProduct(cart, product).orElseGet(() -> {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(0);
            return item;
        });

        cartItem.setQuantity(cartItem.getQuantity() + quantity);
        return toCartItemResponse(cartItemRepository.save(cartItem));
    }

    @Transactional
    public CartItemResponse updateProductQuantity(Long userId, Long productId, Integer quantity) {
        Cart cart = getExistingCart(userId);
        CartItem cartItem = cartItemRepository.findByCartAndProductId(cart, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product is not in cart: " + productId));
        cartItem.setQuantity(quantity);
        return toCartItemResponse(cartItemRepository.save(cartItem));
    }

    @Transactional
    public CartResponse removeProduct(Long userId, Long productId) {
        Cart cart = getExistingCart(userId);
        CartItem cartItem = cartItemRepository.findByCartAndProductId(cart, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product is not in cart: " + productId));
        cartItemRepository.delete(cartItem);
        if (cart.getItems() != null) {
            cart.getItems().removeIf(item -> item.getProduct().getId().equals(productId));
        }
        return toCartResponse(cart);
    }

    @Transactional
    public CartResponse clearCart(Long userId) {
        Cart cart = getExistingCart(userId);
        cartItemRepository.deleteByCart(cart);
        if (cart.getItems() != null) {
            cart.getItems().clear();
        }
        return toCartResponse(cart);
    }

    private Cart getOrCreateCartEntity(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        return cartRepository.findByUserId(userId).orElseGet(() -> {
            Cart newCart = new Cart();
            newCart.setUser(user);
            return cartRepository.save(newCart);
        });
    }

    private Cart getExistingCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userId));
    }

    private CartResponse toCartResponse(Cart cart) {
        return new CartResponse(
                cart.getId(),
                cart.getUser().getId(),
                cart.getItems() == null
                        ? java.util.List.of()
                        : cart.getItems().stream().map(this::toCartItemResponse).toList()
        );
    }

    private CartItemResponse toCartItemResponse(CartItem cartItem) {
        Product product = cartItem.getProduct();
        return new CartItemResponse(
                cartItem.getId(),
                product.getId(),
                product.getName(),
                product.getPrice(),
                cartItem.getQuantity()
        );
    }
}
