package com.market.pricecomparator.service;

import com.market.pricecomparator.dto.PriceAlertMatchDTO;
import com.market.pricecomparator.model.Discount;
import com.market.pricecomparator.model.Product;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PriceAlertService {

    private final ProductService productService;
    private final DiscountService discountService;

    private final String discountsBaseDir = "src/main/resources/data";

    public PriceAlertService(ProductService productService, DiscountService discountService) {
        this.productService = productService;
        this.discountService = discountService;
    }

    public List<PriceAlertMatchDTO> checkPriceAgainstTarget(
            String productName,
            Optional<String> brand,
            Optional<String> store,
            double targetPrice,
            LocalDate date) {

        // Load all products for the current date grouped by store
        Map<String, List<Product>> productsByStore = productService.loadProductsByStore(date);

        // Flatten all products
        List<Product> allProducts = productsByStore.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());

        // Build productMap keyed by productId|store (unique key)
        Map<String, Product> productMap = allProducts.stream()
                .collect(Collectors.toMap(
                        p -> p.getProductId() + "|" + p.getStore().toLowerCase(),
                        p -> p
                ));

        // Load all discounts for current and previous week (filtered by stores or all)
        List<Discount> discounts = discountService.loadDiscountsForCurrentAndPreviousWeek(
                store.map(List::of).orElse(new ArrayList<>(productsByStore.keySet())),
                date,
                discountsBaseDir
        );

        // Build discountMap keyed by productId|store, picking discount that saves the most money
        Map<String, Discount> discountMap = discounts.stream()
                .filter(d -> {
                    // Discount key for productMap lookup
                    String key = d.getProductId() + "|" + d.getStore().toLowerCase();
                    return productMap.containsKey(key);
                })
                .collect(Collectors.toMap(
                        d -> d.getProductId() + "|" + d.getStore().toLowerCase(),
                        d -> d,
                        (d1, d2) -> {
                            String key1 = d1.getProductId() + "|" + d1.getStore().toLowerCase();
                            String key2 = d2.getProductId() + "|" + d2.getStore().toLowerCase();
                            Product p1 = productMap.get(key1);
                            Product p2 = productMap.get(key2);
                            double saving1 = p1.getPrice() * d1.getPercentage() / 100.0;
                            double saving2 = p2.getPrice() * d2.getPercentage() / 100.0;
                            return saving1 >= saving2 ? d1 : d2;
                        }
                ));

        // Filter products by criteria, calculate discounted price, and return matches
        return allProducts.stream()
                .filter(p -> p.getProductName().equalsIgnoreCase(productName))
                .filter(p -> brand.map(b -> b.equalsIgnoreCase(p.getBrand())).orElse(true))
                .filter(p -> store.map(s -> s.equalsIgnoreCase(p.getStore())).orElse(true))
                .map(p -> {
                    String discountKey = p.getProductId() + "|" + p.getStore().toLowerCase();
                    double discountPercent = Optional.ofNullable(discountMap.get(discountKey))
                            .map(Discount::getPercentage)
                            .orElse(0);
                    double discountedPrice = p.getPrice() * (1 - discountPercent / 100.0);
                    boolean matched = discountedPrice <= targetPrice;
                    return new PriceAlertMatchDTO(p, discountedPrice, matched);
                })
                .filter(PriceAlertMatchDTO::isMatched)
                .sorted(Comparator.comparingDouble(PriceAlertMatchDTO::getEffectivePrice))
                .collect(Collectors.toList());
    }
}