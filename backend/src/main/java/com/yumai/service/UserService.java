package com.yumai.service;

import com.yumai.dto.AuthDtos.RegisterRequest;
import com.yumai.dto.AuthDtos.UserDto;
import com.yumai.entity.Role;
import com.yumai.entity.User;
import com.yumai.exception.BadRequestException;
import com.yumai.exception.NotFoundException;
import com.yumai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Admin-only user CRUD (FR-01.4, UC-02). */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserDto> findAll() {
        return userRepository.findAll().stream().map(UserDto::from).toList();
    }

    @Transactional
    public UserDto create(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("An account with this email already exists");
        }
        Role role = request.role() != null ? request.role() : Role.STAFF;
        User user = new User(request.name(), request.email(),
                passwordEncoder.encode(request.password()), role);
        return UserDto.from(userRepository.save(user));
    }

    @Transactional
    public UserDto update(Long id, RegisterRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
        user.setName(request.name());
        if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("An account with this email already exists");
        }
        user.setEmail(request.email());
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return UserDto.from(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id, User caller) {
        if (caller != null && caller.getUserId().equals(id)) {
            throw new BadRequestException("You cannot delete your own account");
        }
        if (!userRepository.existsById(id)) {
            throw new NotFoundException("User not found: " + id);
        }
        userRepository.deleteById(id);
    }
}
