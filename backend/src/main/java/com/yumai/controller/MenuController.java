package com.yumai.controller;

import com.yumai.dto.MenuDtos.MenuItemRequest;
import com.yumai.entity.MenuItem;
import com.yumai.service.MenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** UC-03 - menu item management. */
@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping
    public List<MenuItem> findAll() {
        return menuService.findAll();
    }

    @GetMapping("/{id}")
    public MenuItem findById(@PathVariable Long id) {
        return menuService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MenuItem create(@Valid @RequestBody MenuItemRequest request) {
        return menuService.create(request);
    }

    @PutMapping("/{id}")
    public MenuItem update(@PathVariable Long id, @Valid @RequestBody MenuItemRequest request) {
        return menuService.update(id, request);
    }

    @PatchMapping("/{id}/availability")
    public MenuItem toggleAvailability(@PathVariable Long id) {
        return menuService.toggleAvailability(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        menuService.delete(id);
    }
}
