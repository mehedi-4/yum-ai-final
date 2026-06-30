package com.yumai.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Request records for menu management (UC-03). */
public final class MenuDtos {

    private MenuDtos() {
    }

    public record IngredientRequest(@NotNull Long inventoryItemId, @NotNull @DecimalMin("0.0") Double quantityNeeded) {
    }

    public record MenuItemRequest(
            @NotBlank String name,
            String description,
            @NotNull @DecimalMin("0.0") Double price,
            @DecimalMin("0.0") Double costPrice,
            @NotBlank String category,
            Boolean isAvailable,
            List<IngredientRequest> ingredients) {
    }
}
