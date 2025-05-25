package com.market.pricecomparator.service;

import com.market.pricecomparator.dto.ProductValueDTO;
import com.market.pricecomparator.model.Product;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductRecommendationService {
    // Maps common units to standard base units (kg, l, buc)
    private static final Map<String, Double> UNIT_CONVERSION_TO_STANDARD = Map.of(
            "g", 0.001,
            "kg", 1.0,
            "ml", 0.001,
            "l", 1.0,
            "buc", 1.0
    );
    //Normalize quantity to base unit (e.g., grams -> kg)
    private double normalizeQuantity(double quantity, String unit) {
        return quantity * UNIT_CONVERSION_TO_STANDARD.getOrDefault(unit.toLowerCase(), 1.0);
    }


    private final ProductService productService;

    public ProductRecommendationService(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Find product substitutes/recommendations for a given product (productName, brand), filtered by store.
     * Recommendations are sorted by best value per unit (lowest price per unit).
     */
    public List<ProductValueDTO> findBestValueProducts(
            String productName,
            Optional<String> brandFilter,
            Optional<String> storeFilter,
            LocalDate date) {

        // Load all products by store for the date
        Map<String, List<Product>> productsByStore = productService.loadProductsByStore(date);

        // Flatten all products into one list
        List<Product> allProducts = productsByStore.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());

        // Find base product (first match by productName and brand if given)
        Product baseProduct = allProducts.stream()
                .filter(p -> p.getProductName().equalsIgnoreCase(productName))
                .filter(p -> brandFilter.map(b -> p.getBrand().equalsIgnoreCase(b)).orElse(true))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Base product not found for name: " + productName));
        String baseUnit = baseProduct.getUnit().toLowerCase();

        // Filter substitutes:
        // - same productName
        // - brand filter if specified
        // - same unit for fair comparison
        // - optional store filter
        List<Product> filtered = allProducts.stream()
                .filter(p -> p.getProductName().toLowerCase().equals(productName))
                .filter(p -> brandFilter.map(b -> p.getBrand().toLowerCase().equals(b)).orElse(true))
                .filter(p -> storeFilter.map(s -> p.getStore().toLowerCase().equals(s)).orElse(true))
                .filter(p -> p.getUnit() != null && normalizeQuantity(1, p.getUnit()) > 0)
                .filter(p -> normalizeQuantity(1, p.getUnit()) == normalizeQuantity(1, baseUnit)) // match normalized units
                .collect(Collectors.toList());


        // Compute price per unit based on normalized quantity
        return filtered.stream()
                .map(p -> {
                    double normalizedQuantity = normalizeQuantity(p.getQuantity(), p.getUnit());
                    double pricePerUnit = p.getPrice() / normalizedQuantity;
                    return new ProductValueDTO(p, pricePerUnit);
                })
                .sorted(Comparator.comparing(ProductValueDTO::getPricePerUnit))
                .collect(Collectors.toList());
    }
}
