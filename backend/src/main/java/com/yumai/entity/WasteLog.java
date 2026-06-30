package com.yumai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** SRS 5.1.7 - record of discarded inventory (FR-03.4). */
@Entity
@Table(name = "waste_logs")
@Getter
@Setter
@NoArgsConstructor
public class WasteLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "inventory_item_id")
    private InventoryItem inventoryItem;

    @Column(nullable = false)
    private Double quantity;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime loggedAt = LocalDateTime.now();

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "logged_by")
    private User loggedBy;

    public WasteLog(InventoryItem inventoryItem, Double quantity, String reason, User loggedBy) {
        this.inventoryItem = inventoryItem;
        this.quantity = quantity;
        this.reason = reason;
        this.loggedBy = loggedBy;
    }
}
