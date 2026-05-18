package com.example.ecosystem.dto;

import java.util.List;

public record CartResponse(
        Long id,
        Long userId,
        List<CartItemResponse> items
) {
}
