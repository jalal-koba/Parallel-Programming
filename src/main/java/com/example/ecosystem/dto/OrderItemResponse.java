package com.example.ecosystem.dto;

public record OrderItemResponse(
        Long id,
        Long productId,
        String productName,
        Integer quantity,
        Float unitPriceAtPurchase
) {
}
