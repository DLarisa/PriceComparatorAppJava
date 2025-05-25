package com.market.pricecomparator.dto;

import com.market.pricecomparator.model.Product;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PriceAlertMatchDTO {
    private Product product;
    private double effectivePrice;
    private boolean matched; // true if price <= target
}
