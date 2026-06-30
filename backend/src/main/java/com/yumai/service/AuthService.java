package com.yumai.service;

import com.yumai.dto.AuthDtos.AuthResponse;
import com.yumai.dto.AuthDtos.LoginRequest;
import com.yumai.dto.AuthDtos.RegisterRequest;
import com.yumai.dto.AuthDtos.UserDto;
import com.yumai.entity.Role;
import com.yumai.entity.User;
import com.yumai.exception.BadRequestException;
import com.yumai.repository.UserRepository;
import com.yumai.security.JwtService;
import com.yumai.security.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Authentication: registration, JWT login and logout (FR-01). */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService blacklistService;

    /**
     * FR-01.1 - register with name, email, password and role.
     * The very first account becomes ADMIN (bootstrap); afterwards the requested
     * role is honored only when an Admin performs the registration, otherwise the
     * account is created as STAFF (see DEVIATIONS.md D4).
     */
    @Transactional
    public AuthResponse register(RegisterRequest request, User caller) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("An account with this email already exists");
        }
        Role role;
        if (userRepository.count() == 0) {
            role = Role.ADMIN;
        } else if (caller != null && caller.getRole() == Role.ADMIN) {
            role = request.role() != null ? request.role() : Role.STAFF;
        } else {
            role = Role.STAFF;
        }
        User user = new User(request.name(), request.email(),
                passwordEncoder.encode(request.password()), role);
        userRepository.save(user);
        return new AuthResponse(jwtService.generateToken(user), UserDto.from(user));
    }

    /** FR-01.2 - authenticate and issue a JWT. */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        return new AuthResponse(jwtService.generateToken(user), UserDto.from(user));
    }

    /** FR-01.5 - invalidate the JWT on logout. */
    public void logout(String token) {
        Claims claims = jwtService.parse(token);
        if (claims != null) {
            blacklistService.blacklist(token, claims.getExpiration().toInstant());
        }
    }
}
