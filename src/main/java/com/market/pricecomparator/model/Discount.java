package com.market.pricecomparator.model;

import lombok.*;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Discount extends Product{
    private LocalDate fromDate;
    private LocalDate toDate;
    private int percentage;

    public boolean isApplicable(LocalDate date) {
        return (date.isEqual(fromDate) || date.isAfter(fromDate)) &&
                (date.isEqual(toDate) || date.isBefore(toDate));
    }

    @Override
    public String toString() {
        return String.format(
                "Discount{productId='%s', productName='%s', brand='%s', quantity=%.2f, unit='%s', currency='%s', fromDate=%s, toDate=%s, percentage=%d, store='%s'}",
                getProductId(),
                getProductName(),
                getBrand(),
                getQuantity(),
                getUnit(),
                getCurrency(),
                getFromDate(),
                getToDate(),
                getPercentage(),
                getStore()
        );
    }
}
