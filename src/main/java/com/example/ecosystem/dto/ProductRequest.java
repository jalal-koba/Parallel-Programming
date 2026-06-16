package com.example.ecosystem.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductRequest(
        @NotBlank String name,
        String description,
        @NotNull @Min(0) Float price,
        @NotNull @Min(0) Integer stockQuantity,
        Long categoryId
) {
}
