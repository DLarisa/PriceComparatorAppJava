package com.market.pricecomparator.service;

import com.market.pricecomparator.dto.BestDiscountsRequestDTO;
import com.market.pricecomparator.dto.ProductDiscountDTO;
import com.market.pricecomparator.model.Discount;
import com.market.pricecomparator.model.Product;
import com.market.pricecomparator.util.CsvLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Should add pagination or caching [For very large lists, the sorting cost grows.].
 */

@Service
public class DiscountService {
    private final CsvLoader csvLoader;
    private final ProductService productService;
    private final String discountsBaseDir = "src/main/resources/data";

    public DiscountService(CsvLoader csvLoader, ProductService productService) {
        this.csvLoader = csvLoader;
        this.productService = productService;
    }

    /**
     * Loads discounts for the current week and previous week based on currentDate.
     * Assumes discount CSV files are named with store and date, e.g. lidl_discounts_2025-05-01.csv
     *
     * @param stores       List of store names to load discounts for
     * @param currentDate  The current date to base loading weeks on
     * @param baseDirPath  Base directory where discount CSV files are stored
     * @return List of discounts active for current or previous week
     */
    public List<Discount> loadDiscountsForCurrentAndPreviousWeek(List<String> stores, LocalDate currentDate, String baseDirPath) {
        List<Discount> allDiscounts = new ArrayList<>();

        // Calculate previous week date (assuming 7 days back)
        LocalDate previousWeekDate = currentDate.minusDays(7);

        for (String store : stores) {
            String currentWeekFile = String.format("%s/%s_discounts_%s.csv", baseDirPath, store, currentDate);
            String previousWeekFile = String.format("%s/%s_discounts_%s.csv", baseDirPath, store, previousWeekDate);

            // Load current week discounts if file exists
            allDiscounts.addAll(csvLoader.loadDiscounts(currentWeekFile));
            // Load previous week discounts if file exists
            allDiscounts.addAll(csvLoader.loadDiscounts(previousWeekFile));
        }

        // Filter out discounts that are not relevant for the current date period
        // (optional safety check: keep discounts that overlap with currentDate or previous week)
        LocalDate previousWeekStart = currentDate.minusDays(7);
        LocalDate currentWeekEnd = currentDate.plusDays(6);

        return allDiscounts.stream()
                .filter(discount -> !(discount.getToDate().isBefore(previousWeekStart) || discount.getFromDate().isAfter(currentWeekEnd)))
                .collect(Collectors.toList());
    }

    /**
     * Get top N discounts filtered by productName and optional brand for a given date.
     */
    public List<ProductDiscountDTO> getBestDiscounts(LocalDate date, BestDiscountsRequestDTO filter) {
        // Load products per store for date
        Map<String, List<Product>> productsByStore = productService.loadProductsByStore(date);

        List<ProductDiscountDTO> results = new ArrayList<>();

        for (Map.Entry<String, List<Product>> storeEntry : productsByStore.entrySet()) {
            String store = storeEntry.getKey();
            List<Product> products = storeEntry.getValue();

            // Load discounts for this store and week range (current and previous week)
            List<Discount> discounts = loadDiscountsForCurrentAndPreviousWeek(
                    List.of(store), date, discountsBaseDir);

            // Map products by productId for quick lookup
            Map<String, Product> productIdMap = products.stream()
                    .collect(Collectors.toMap(Product::getProductId, p -> p));

            for (Discount discount : discounts) {
                Product product = productIdMap.get(discount.getProductId());
                if (product == null) continue;

                // Filter by productName (mandatory)
                if (!product.getProductName().equalsIgnoreCase(filter.getProductName())) continue;

                // Filter by brand if specified
                if (filter.getBrand() != null && !filter.getBrand().isBlank()
                        && !product.getBrand().equalsIgnoreCase(filter.getBrand())) continue;

                // Create DTO with discount percentage from discount record
                results.add(buildDTO(product, discount, store));
            }
        }

        // Sort descending by discountPercentage and limit top N using enum value
        return sortAndLimitTopN(results, filter.getTopN().getValue());
    }

    /**
     * Get top N discounts filtered by store.
     */
    public List<ProductDiscountDTO> getTopDiscountsForStore(LocalDate date, String store, BestDiscountsRequestDTO filter) {
        // Load all products for that store and date
        List<Product> products = productService.loadProductsByStore(date).getOrDefault(store, List.of());
        if (products.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No products found for store: " + store);
        }

        // Load discounts for that store for current and previous week
        List<Discount> discounts = loadDiscountsForCurrentAndPreviousWeek(
                List.of(store), date, discountsBaseDir);
        Map<String, Product> productIdMap = products.stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p));

        List<ProductDiscountDTO> results = buildDiscountDTOs(productIdMap, discounts, store);
        return sortAndLimitTopN(results, filter.getTopN().getValue());
    }

    /**
     * Get top N discounts filtered by all stores.
     */
    public List<ProductDiscountDTO> getTopDiscountsAcrossStores(LocalDate date, BestDiscountsRequestDTO filter) {
        Map<String, List<Product>> productsByStore = productService.loadProductsByStore(date);

        List<ProductDiscountDTO> results = new ArrayList<>();

        for (Map.Entry<String, List<Product>> entry : productsByStore.entrySet()) {
            String store = entry.getKey();
            List<Product> products = entry.getValue();

            List<Discount> discounts = loadDiscountsForCurrentAndPreviousWeek(List.of(store), date, discountsBaseDir);
            Map<String, Product> productIdMap = products.stream()
                    .collect(Collectors.toMap(Product::getProductId, p -> p));

            results.addAll(buildDiscountDTOs(productIdMap, discounts, store));
        }

        return sortAndLimitTopN(results, filter.getTopN().getValue());
    }

    private List<ProductDiscountDTO> buildDiscountDTOs(Map<String, Product> productIdMap, List<Discount> discounts, String store) {
        return discounts.stream()
                .map(discount -> {
                    Product product = productIdMap.get(discount.getProductId());
                    return product != null ? buildDTO(product, discount, store) : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private ProductDiscountDTO buildDTO(Product product, Discount discount, String store) {
        return new ProductDiscountDTO(
                product.getProductId(),
                product.getProductName(),
                product.getBrand(),
                product.getPrice(),
                discount.getPercentage(),
                discount.getFromDate(),
                discount.getToDate(),
                store
        );
    }

    public List<ProductDiscountDTO> getNewDiscounts(LocalDate referenceDate, BestDiscountsRequestDTO filter) {
        int days = (filter.getNewWithinDays() != null) ? filter.getNewWithinDays() : 1;

        if (days < 1 || days > 14) {
            throw new IllegalArgumentException("newWithinDays must be between 1 and 14");
        }

        // Calculate earliest fromDate to consider as new
        LocalDate earliestNewDate = referenceDate.minusDays(days);

        // Load products per store for date
        Map<String, List<Product>> productsByStore = productService.loadProductsByStore(referenceDate);

        List<ProductDiscountDTO> newDiscounts = new ArrayList<>();

        for (Map.Entry<String, List<Product>> entry : productsByStore.entrySet()) {
            String store = entry.getKey();
            List<Product> products = entry.getValue();

            // Load discounts for this store and week range (current and previous week)
            List<Discount> discounts = loadDiscountsForCurrentAndPreviousWeek(List.of(store), referenceDate, discountsBaseDir);

            // Map products by productId for quick lookup
            Map<String, Product> productIdMap = products.stream()
                    .collect(Collectors.toMap(Product::getProductId, p -> p));

            for (Discount discount : discounts) {
                if (discount.getFromDate() != null &&
                        !discount.getFromDate().isBefore(earliestNewDate) &&
                        !discount.getFromDate().isAfter(referenceDate)) { // fromDate in [earliestNewDate, referenceDate]

                    Product product = productIdMap.get(discount.getProductId());
                    if (product == null) continue;

                    // Optional filtering by productName (if specified)
                    if (filter.getProductName() != null && !filter.getProductName().isBlank() &&
                            !product.getProductName().equalsIgnoreCase(filter.getProductName())) {
                        continue;
                    }

                    // Optional filtering by brand (if specified)
                    if (filter.getBrand() != null && !filter.getBrand().isBlank() &&
                            !product.getBrand().equalsIgnoreCase(filter.getBrand())) {
                        continue;
                    }

                    newDiscounts.add(buildDTO(product, discount, store));
                }
            }
        }

        return sortAndLimitTopN(newDiscounts, filter.getTopN().getValue());
    }

    /**
     * Sorting first by discount percentage descending,
     * and then by absolute money saved (price * discount percentage)
     * descending will prioritize products that give customers the biggest monetary
     * benefit when discounts are equal.
     */
    private List<ProductDiscountDTO> sortAndLimitTopN(List<ProductDiscountDTO> discounts, int topN) {
        return discounts.stream()
                .sorted(Comparator
                        .comparing(ProductDiscountDTO::getDiscountPercentage).reversed()
                        .thenComparing(dto -> dto.getPrice() * dto.getDiscountPercentage() / 100.0, Comparator.reverseOrder())
                )
                .limit(topN)
                .collect(Collectors.toList());
    }
}
