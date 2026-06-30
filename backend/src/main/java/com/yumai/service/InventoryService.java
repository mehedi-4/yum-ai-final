package com.yumai.service;

import com.yumai.dto.InventoryDtos.InventoryItemRequest;
import com.yumai.dto.InventoryDtos.WasteLogRequest;
import com.yumai.entity.InventoryItem;
import com.yumai.entity.User;
import com.yumai.entity.WasteLog;
import com.yumai.exception.BadRequestException;
import com.yumai.exception.NotFoundException;
import com.yumai.repository.InventoryItemRepository;
import com.yumai.repository.WasteLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Inventory tracking, low-stock alerts and waste logging (FR-03). */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final WasteLogRepository wasteLogRepository;

    public List<InventoryItem> findAll() {
        return inventoryItemRepository.findAll();
    }

    public InventoryItem findById(Long id) {
        return inventoryItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Inventory item not found: " + id));
    }

    /** FR-03.2 - items at or below their threshold. */
    public List<InventoryItem> findLowStock() {
        return inventoryItemRepository.findLowStock();
    }

    @Transactional
    public InventoryItem create(InventoryItemRequest request) {
        InventoryItem item = new InventoryItem(request.name(), request.quantity(), request.unit(),
                request.category(), request.lowStockThreshold());
        return inventoryItemRepository.save(item);
    }

    @Transactional
    public InventoryItem update(Long id, InventoryItemRequest request) {
        InventoryItem item = findById(id);
        item.setName(request.name());
        item.setQuantity(request.quantity());
        item.setUnit(request.unit());
        item.setCategory(request.category());
        item.setLowStockThreshold(request.lowStockThreshold());
        item.setLastUpdated(LocalDateTime.now());
        return inventoryItemRepository.save(item);
    }

    /** FR-03.3 - manual stock adjustment by Manager/Admin. */
    @Transactional
    public InventoryItem updateStock(Long id, Double quantity) {
        InventoryItem item = findById(id);
        item.setQuantity(quantity);
        item.setLastUpdated(LocalDateTime.now());
        return inventoryItemRepository.save(item);
    }

    @Transactional
    public void delete(Long id) {
        if (!inventoryItemRepository.existsById(id)) {
            throw new NotFoundException("Inventory item not found: " + id);
        }
        inventoryItemRepository.deleteById(id);
    }

    /** FR-03.4 - record discarded inventory and deduct it from stock. */
    @Transactional
    public WasteLog logWaste(WasteLogRequest request, User loggedBy) {
        InventoryItem item = findById(request.inventoryItemId());
        if (request.quantity() > item.getQuantity()) {
            throw new BadRequestException("Waste quantity exceeds current stock of " + item.getName());
        }
        item.setQuantity(item.getQuantity() - request.quantity());
        item.setLastUpdated(LocalDateTime.now());
        inventoryItemRepository.save(item);
        return wasteLogRepository.save(new WasteLog(item, request.quantity(), request.reason(), loggedBy));
    }

    public List<WasteLog> findWasteLogs() {
        return wasteLogRepository.findAllByOrderByLoggedAtDesc();
    }
}
