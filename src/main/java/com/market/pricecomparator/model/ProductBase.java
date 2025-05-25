package com.market.pricecomparator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductBase {
    private String productId;
    private String productName;
    private String brand;
    private String category;
    private double quantity;
    private String unit;
    private String store;
}
