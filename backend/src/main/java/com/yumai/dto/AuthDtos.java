package com.yumai.dto;

import com.yumai.entity.Role;
import com.yumai.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** Request/response records for authentication (FR-01). */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank String name,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, message = "Password must be at least 6 characters") String password,
            Role role) {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record UserDto(Long userId, String name, String email, Role role, LocalDateTime createdAt) {
        public static UserDto from(User u) {
            return new UserDto(u.getUserId(), u.getName(), u.getEmail(), u.getRole(), u.getCreatedAt());
        }
    }

    public record AuthResponse(String token, UserDto user) {
    }
}
