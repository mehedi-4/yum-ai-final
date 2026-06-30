package com.yumai.controller;

import com.yumai.dto.AuthDtos.AuthResponse;
import com.yumai.dto.AuthDtos.LoginRequest;
import com.yumai.dto.AuthDtos.RegisterRequest;
import com.yumai.dto.AuthDtos.UserDto;
import com.yumai.entity.User;
import com.yumai.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** FR-01 - registration, JWT login/logout, current profile. */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request,
                                 @AuthenticationPrincipal User caller) {
        return authService.register(request, caller);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader(value = "Authorization", required = false) String header) {
        if (header != null && header.startsWith("Bearer ")) {
            authService.logout(header.substring(7));
        }
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal User user) {
        return UserDto.from(user);
    }
}
