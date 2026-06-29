package com.yumai.controller;

import com.yumai.dto.OrderDtos.OrderRequest;
import com.yumai.entity.Order;
import com.yumai.entity.User;
import com.yumai.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** FR-02 / UC-04, UC-05 - order lifecycle. */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public List<Order> findAll() {
        return orderService.findAll();
    }

    @GetMapping("/{id}")
    public Order findById(@PathVariable Long id) {
        return orderService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order create(@Valid @RequestBody OrderRequest request, @AuthenticationPrincipal User user) {
        return orderService.create(request, user);
    }

    @PutMapping("/{id}")
    public Order update(@PathVariable Long id, @Valid @RequestBody OrderRequest request) {
        return orderService.update(id, request);
    }

    @PostMapping("/{id}/complete")
    public Order complete(@PathVariable Long id) {
        return orderService.complete(id);
    }

    @PostMapping("/{id}/cancel")
    public Order cancel(@PathVariable Long id) {
        return orderService.cancel(id);
    }
}
