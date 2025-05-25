package com.market.pricecomparator.dto;

import com.market.pricecomparator.model.Product;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class StoreProductsDTO {
    private String store;
    private List<Product> products;
}
