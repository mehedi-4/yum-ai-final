package com.yumai.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Join entity for the MenuItem <-> InventoryItem many-to-many (SRS 5.2),
 * carrying the ingredient quantity consumed per unit sold. Drives the
 * automatic stock deduction on order completion (FR-02.6).
 */
@Entity
@Table(name = "menu_ingredients")
@Getter
@Setter
@NoArgsConstructor
public class MenuIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id")
    private MenuItem menuItem;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "inventory_item_id")
    private InventoryItem inventoryItem;

    /** Quantity of the ingredient (in its unit) used per single menu item. */
    @Column(nullable = false)
    private Double quantityNeeded;

    public MenuIngredient(MenuItem menuItem, InventoryItem inventoryItem, Double quantityNeeded) {
        this.menuItem = menuItem;
        this.inventoryItem = inventoryItem;
        this.quantityNeeded = quantityNeeded;
    }
}
