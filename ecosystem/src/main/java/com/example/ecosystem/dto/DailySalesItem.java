package com.example.ecosystem.dto;

import java.math.BigDecimal;

public record DailySalesItem(
        Long orderId,
        Long productId,
        String productName,
        int quantity,
        BigDecimal revenue
) {
}
