package com.market.pricecomparator.dto;

import com.market.pricecomparator.model.Product;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductValueDTO {
    private Product product;
    private double pricePerUnit;  // price per unit (e.g. RON/kg, RON/l)
}