package com.market.pricecomparator.service;

import com.market.pricecomparator.dto.BasketOptimizationResultDTO;
import com.market.pricecomparator.dto.ShoppingItemDTO;
import com.market.pricecomparator.dto.StoreProductsDTO;
import com.market.pricecomparator.model.Discount;
import com.market.pricecomparator.model.Product;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BasketOptimizerService {
    /**
     * Optimize the shopping basket with discounts applied and return detailed result DTO.
     *
     * @param shoppingList    list of items user wants
     * @param productsByStore map of store -> products (base prices)
     * @param allDiscounts    list of discounts (from current and previous weeks)
     * @param currentDate     date for discount application
     * @return detailed optimization result DTO with totalCost, unmatchedItems, and timestamp
     */
    public BasketOptimizationResultDTO optimizeBasketDetailed(
            List<ShoppingItemDTO> shoppingList,
            Map<String, List<Product>> productsByStore,
            List<Discount> allDiscounts,
            LocalDate currentDate
    ) {
        Map<String, Product> cheapestProductByKey = new HashMap<>();
        List<ShoppingItemDTO> unmatchedItems = new ArrayList<>();
        Map<Product, Double> productEffectivePriceMap = new HashMap<>();
        Map<Product, Double> productBasePriceMap = new HashMap<>();

        // Deduplicate shopping list based on productName and optional brand
        Set<String> seenKeys = new HashSet<>();
        List<ShoppingItemDTO> deduplicatedShoppingList = new ArrayList<>();

        for (ShoppingItemDTO item : shoppingList) {
            String nameKey = item.getProductName().toLowerCase().trim();
            String brandKey = (item.getBrand() != null && !item.getBrand().isBlank())
                    ? item.getBrand().toLowerCase().trim()
                    : "";

            String key = nameKey + "|" + brandKey;

            if (seenKeys.add(key)) {
                deduplicatedShoppingList.add(item);
            }
        }

        for (ShoppingItemDTO item : deduplicatedShoppingList) {
            String desiredName = item.getProductName().toLowerCase().trim();
            String desiredBrand = item.getBrand() != null ? item.getBrand().toLowerCase().trim() : "";

            Product cheapestProduct = null;
            double cheapestPrice = Double.MAX_VALUE;

            for (List<Product> storeProducts : productsByStore.values()) {
                for (Product product : storeProducts) {
                    if (isMatch(product, desiredName, desiredBrand)) {
                        double effectivePrice = getEffectivePrice(product, allDiscounts, currentDate);
                        if (effectivePrice < cheapestPrice) {
                            cheapestPrice = effectivePrice;
                            cheapestProduct = product;
                        }
                    }
                }
            }

            if (cheapestProduct != null) {
                String key = desiredName + (desiredBrand.isEmpty() ? "" : "_" + desiredBrand);
                cheapestProductByKey.put(key, cheapestProduct);
                productEffectivePriceMap.put(cheapestProduct, cheapestPrice);
                productBasePriceMap.put(cheapestProduct, cheapestProduct.getPrice());
            } else {
                unmatchedItems.add(item);
            }
        }

        Map<String, List<Product>> groupedByStore = cheapestProductByKey.values().stream()
                .collect(Collectors.groupingBy(Product::getStore));

        List<StoreProductsDTO> stores = groupedByStore.entrySet().stream()
                .map(entry -> new StoreProductsDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        double totalCost = productEffectivePriceMap.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        // Calculate total savings (basePrice - effectivePrice)
        double totalSavings = productEffectivePriceMap.entrySet().stream()
                .mapToDouble(e -> productBasePriceMap.get(e.getKey()) - e.getValue())
                .sum();

        // Round to 2 decimal places
        totalCost = BigDecimal.valueOf(totalCost)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();

        totalSavings = BigDecimal.valueOf(totalSavings)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();

        return new BasketOptimizationResultDTO(
                stores,
                totalCost,
                unmatchedItems,
                totalSavings,
                LocalDateTime.now()
        );
    }

    private boolean isMatch(Product product, String desiredName, String desiredBrand) {
        String productName = product.getProductName().toLowerCase().trim();
        String productBrand = product.getBrand() != null ? product.getBrand().toLowerCase().trim() : "";

        if (!desiredBrand.isEmpty()) {
            return productName.equals(desiredName) && productBrand.equals(desiredBrand);
        } else {
            return productName.equals(desiredName);
        }
    }

    private double getEffectivePrice(Product product, List<Discount> allDiscounts, LocalDate currentDate) {
        double basePrice = product.getPrice();

        return allDiscounts.stream()
                .filter(d -> d.getProductId().equals(product.getProductId()))
                .filter(d -> d.getStore().equals(product.getStore()))
                .filter(d -> d.isApplicable(currentDate))
                .map(d -> basePrice * (1 - d.getPercentage() / 100.0))
                .min(Double::compare)
                .orElse(basePrice);
    }
}