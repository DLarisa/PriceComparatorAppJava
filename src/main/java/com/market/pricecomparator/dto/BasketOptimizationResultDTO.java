package com.market.pricecomparator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class BasketOptimizationResultDTO {
    private List<StoreProductsDTO> stores;
    private double totalCost;
    private List<ShoppingItemDTO> unmatchedItems;
    private double totalSavings;
    private LocalDateTime timestamp;
}
