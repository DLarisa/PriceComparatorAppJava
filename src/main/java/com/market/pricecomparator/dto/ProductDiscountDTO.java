package com.market.pricecomparator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDiscountDTO {
    private String productId;
    private String productName;
    private String brand;
    private double price;
    private int discountPercentage;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String store;
}
