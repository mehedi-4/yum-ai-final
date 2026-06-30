package com.yumai.service;

import com.yumai.dto.MenuDtos.IngredientRequest;
import com.yumai.dto.MenuDtos.MenuItemRequest;
import com.yumai.entity.InventoryItem;
import com.yumai.entity.MenuIngredient;
import com.yumai.entity.MenuItem;
import com.yumai.exception.NotFoundException;
import com.yumai.repository.InventoryItemRepository;
import com.yumai.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Menu item CRUD and ingredient mapping (UC-03, SRS 5.1.4). */
@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuItemRepository menuItemRepository;
    private final InventoryItemRepository inventoryItemRepository;

    public List<MenuItem> findAll() {
        return menuItemRepository.findAll();
    }

    public MenuItem findById(Long id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Menu item not found: " + id));
    }

    @Transactional
    public MenuItem create(MenuItemRequest request) {
        MenuItem item = new MenuItem(request.name(), request.description(), request.price(),
                request.costPrice() != null ? request.costPrice() : 0.0, request.category());
        if (request.isAvailable() != null) {
            item.setIsAvailable(request.isAvailable());
        }
        applyIngredients(item, request.ingredients());
        return menuItemRepository.save(item);
    }

    @Transactional
    public MenuItem update(Long id, MenuItemRequest request) {
        MenuItem item = findById(id);
        item.setName(request.name());
        item.setDescription(request.description());
        item.setPrice(request.price());
        if (request.costPrice() != null) {
            item.setCostPrice(request.costPrice());
        }
        item.setCategory(request.category());
        if (request.isAvailable() != null) {
            item.setIsAvailable(request.isAvailable());
        }
        applyIngredients(item, request.ingredients());
        return menuItemRepository.save(item);
    }

    @Transactional
    public MenuItem toggleAvailability(Long id) {
        MenuItem item = findById(id);
        item.setIsAvailable(!item.getIsAvailable());
        return menuItemRepository.save(item);
    }

    @Transactional
    public void delete(Long id) {
        if (!menuItemRepository.existsById(id)) {
            throw new NotFoundException("Menu item not found: " + id);
        }
        menuItemRepository.deleteById(id);
    }

    private void applyIngredients(MenuItem item, List<IngredientRequest> ingredients) {
        if (ingredients == null) {
            return;
        }
        item.getIngredients().clear();
        for (IngredientRequest req : ingredients) {
            InventoryItem inv = inventoryItemRepository.findById(req.inventoryItemId())
                    .orElseThrow(() -> new NotFoundException("Inventory item not found: " + req.inventoryItemId()));
            item.getIngredients().add(new MenuIngredient(item, inv, req.quantityNeeded()));
        }
    }
}
