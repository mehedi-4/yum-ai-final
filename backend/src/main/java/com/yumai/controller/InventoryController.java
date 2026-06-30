package com.yumai.controller;

import com.yumai.dto.InventoryDtos.InventoryItemRequest;
import com.yumai.dto.InventoryDtos.StockUpdateRequest;
import com.yumai.dto.InventoryDtos.WasteLogRequest;
import com.yumai.entity.InventoryItem;
import com.yumai.entity.User;
import com.yumai.entity.WasteLog;
import com.yumai.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** FR-03 / UC-07, UC-08, UC-09 - inventory, alerts and waste logs. */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public List<InventoryItem> findAll() {
        return inventoryService.findAll();
    }

    /** FR-03.2 / UC-08 */
    @GetMapping("/low-stock")
    public List<InventoryItem> lowStock() {
        return inventoryService.findLowStock();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryItem create(@Valid @RequestBody InventoryItemRequest request) {
        return inventoryService.create(request);
    }

    @PutMapping("/{id}")
    public InventoryItem update(@PathVariable Long id, @Valid @RequestBody InventoryItemRequest request) {
        return inventoryService.update(id, request);
    }

    /** FR-03.3 */
    @PatchMapping("/{id}/stock")
    public InventoryItem updateStock(@PathVariable Long id, @Valid @RequestBody StockUpdateRequest request) {
        return inventoryService.updateStock(id, request.quantity());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        inventoryService.delete(id);
    }

    /** FR-03.4 / UC-09 */
    @GetMapping("/waste-logs")
    public List<WasteLog> wasteLogs() {
        return inventoryService.findWasteLogs();
    }

    @PostMapping("/waste-logs")
    @ResponseStatus(HttpStatus.CREATED)
    public WasteLog logWaste(@Valid @RequestBody WasteLogRequest request, @AuthenticationPrincipal User user) {
        return inventoryService.logWaste(request, user);
    }
}
