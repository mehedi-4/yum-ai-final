package com.yumai.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Request records for orders and billing (FR-02). */
public final class OrderDtos {

    private OrderDtos() {
    }

    public record OrderItemRequest(@NotNull Long menuItemId, @NotNull @Min(1) Integer quantity) {
    }

    public record OrderRequest(
            String tableNumber,
            @DecimalMin("0.0") @DecimalMax(value = "100.0", message = "Discount is a percentage (0-100)")
            Double discountPercent,
            @NotEmpty List<OrderItemRequest> items) {
    }
}
