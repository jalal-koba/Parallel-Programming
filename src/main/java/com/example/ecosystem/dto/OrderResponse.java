package com.example.ecosystem.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long userId,
        LocalDateTime createdAt,
        String status,
        Float totalAmount,
        List<OrderItemResponse> items
) {
}
