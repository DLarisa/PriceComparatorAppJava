package com.market.pricecomparator.model;

import lombok.*;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Product extends ProductBase {
    private double price;
    private String currency;
    private LocalDate date;

    public Product(String productId, String productName, String category, String brand, double quantity, String unit,
                   double price, String currency, String store, LocalDate date) {
        super(productId, productName, brand, category, quantity, unit, store);
        this.price = price;
        this.currency = currency;
        this.date = date;
    }

    @Override
    public String toString() {
        return String.format(
                "Product{productId='%s', productName='%s', category='%s', brand='%s', quantity=%.2f, unit='%s', price=%.2f, currency='%s', store='%s', date=%s}",
                getProductId(),
                getProductName(),
                getCategory(),
                getBrand(),
                getQuantity(),
                getUnit(),
                getPrice(),
                getCurrency(),
                getStore(),
                getDate()
        );
    }
}
