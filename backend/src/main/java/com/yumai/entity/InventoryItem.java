package com.yumai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** SRS 5.1.5 - stock-tracked ingredient with low-stock threshold (FR-03). */
@Entity
@Table(name = "inventory_items")
@Getter
@Setter
@NoArgsConstructor
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long itemId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double quantity;

    @Column(nullable = false)
    private String unit;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private Double lowStockThreshold;

    @Column(nullable = false)
    private LocalDateTime lastUpdated = LocalDateTime.now();

    public InventoryItem(String name, Double quantity, String unit, String category, Double lowStockThreshold) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.category = category;
        this.lowStockThreshold = lowStockThreshold;
    }

    /** FR-03.2 */
    public boolean isLowStock() {
        return quantity <= lowStockThreshold;
    }
}
