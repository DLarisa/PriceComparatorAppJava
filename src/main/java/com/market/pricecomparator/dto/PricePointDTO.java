package com.market.pricecomparator.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class PricePointDTO {
    private LocalDate date;
    private double price;
    private int productCount;

    public PricePointDTO(LocalDate date, double price, int productCount) {
        this.date = date;
        this.price = price;
        this.productCount = productCount;
    }
}