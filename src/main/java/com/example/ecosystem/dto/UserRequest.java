package com.example.ecosystem.dto;

import com.example.ecosystem.Entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserRequest(
        @NotBlank String username,
        @Email @NotBlank String email,
        @NotBlank String password,
        Role role
) {
}
