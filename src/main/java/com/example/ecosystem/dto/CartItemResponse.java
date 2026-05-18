package com.example.ecosystem.dto;

public record CartItemResponse(
        Long id,
        Long productId,
        String productName,
        Float unitPrice,
        Integer quantity
) {
}
