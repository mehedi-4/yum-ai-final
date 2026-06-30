package com.yumai.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Request records for inventory and waste logging (FR-03). */
public final class InventoryDtos {

    private InventoryDtos() {
    }

    public record InventoryItemRequest(
            @NotBlank String name,
            @NotNull @DecimalMin("0.0") Double quantity,
            @NotBlank String unit,
            @NotBlank String category,
            @NotNull @DecimalMin("0.0") Double lowStockThreshold) {
    }

    public record StockUpdateRequest(@NotNull @DecimalMin("0.0") Double quantity) {
    }

    public record WasteLogRequest(
            @NotNull Long inventoryItemId,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) Double quantity,
            @NotBlank String reason) {
    }
}
