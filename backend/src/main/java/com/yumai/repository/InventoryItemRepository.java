package com.yumai.repository;

import com.yumai.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    List<InventoryItem> findByCategory(String category);

    /** FR-03.2 - items at or below their low-stock threshold. */
    @Query("select i from InventoryItem i where i.quantity <= i.lowStockThreshold")
    List<InventoryItem> findLowStock();
}
