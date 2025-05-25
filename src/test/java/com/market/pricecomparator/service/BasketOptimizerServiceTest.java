package com.market.pricecomparator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.market.pricecomparator.dto.BasketOptimizationResultDTO;
import com.market.pricecomparator.dto.ShoppingItemDTO;
import com.market.pricecomparator.dto.StoreProductsDTO;
import com.market.pricecomparator.model.Discount;
import com.market.pricecomparator.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class BasketOptimizerServiceTest {
    BasketOptimizerService service = new BasketOptimizerService();

    @BeforeEach
    void setup() {
        service = new BasketOptimizerService();
    }

    @Test
    void testOptimizeBasket_withAndWithoutBrand() throws JsonProcessingException {
        // Mock shopping list
        List<ShoppingItemDTO> shoppingList = Arrays.asList(
                new ShoppingItemDTO("lapte UHT", ""),           // should match both stores
                new ShoppingItemDTO("detergent lichid", "Ariel"), // brand-specific match
                new ShoppingItemDTO("aspirator", "")
        );

        // Mock product list per store
        Map<String, List<Product>> productsByStore = new HashMap<>();

        List<Product> kauflandProducts = Arrays.asList(
                new Product(9.20, "RON", LocalDate.of(2025, 5, 1)) {{
                    setProductId("P777");
                    setProductName("lapte UHT");
                    setCategory("lactate");
                    setBrand("laDorna");
                    setQuantity(1);
                    setUnit("l");
                    setStore("kaufland");
                }},
                new Product(5.20, "RON", LocalDate.of(2025, 5, 1)) {{
                    setProductId("P778");
                    setProductName("lapte UHT");
                    setCategory("lactate");
                    setBrand("Pilos");
                    setQuantity(1);
                    setUnit("l");
                    setStore("kaufland");
                }},
                new Product(50.50, "RON", LocalDate.of(2025, 5, 1)) {{
                    setProductId("P038");
                    setProductName("detergent lichid");
                    setCategory("produse de menaj");
                    setBrand("Ariel");
                    setQuantity(2.5);
                    setUnit("l");
                    setStore("kaufland");
                }}
        );

        List<Product> lidlProducts = Arrays.asList(
                new Product(9.10, "RON", LocalDate.of(2025, 5, 1)) {{
                    setProductId("P777");
                    setProductName("lapte UHT");
                    setCategory("lactate");
                    setBrand("laDorna");
                    setQuantity(1);
                    setUnit("l");
                    setStore("lidl");
                }},
                new Product(5.00, "RON", LocalDate.of(2025, 5, 1)) {{
                    setProductId("P778");
                    setProductName("lapte UHT");
                    setCategory("lactate");
                    setBrand("Pilos");
                    setQuantity(1);
                    setUnit("l");
                    setStore("lidl");
                }},
                new Product(49.90, "RON", LocalDate.of(2025, 5, 1)) {{
                    setProductId("P037");
                    setProductName("detergent lichid");
                    setCategory("produse de menaj");
                    setBrand("Persil");
                    setQuantity(2.5);
                    setUnit("l");
                    setStore("lidl");
                }}
        );

        productsByStore.put("kaufland", kauflandProducts);
        productsByStore.put("lidl", lidlProducts);

        // Discounts from current and previous week (some apply, some don't)
        List<Discount> allDiscounts = Arrays.asList(
                // Discount for P778 lapte UHT Pilos at Kaufland for current week 5%
                new Discount() {{
                    setProductId("P778");
                    setProductName("lapte UHT");
                    setBrand("Pilos");
                    setStore("kaufland");
                    setFromDate(LocalDate.of(2025, 5, 1));
                    setToDate(LocalDate.of(2025, 5, 9));
                    setPercentage(5);
                }},
                // Discount for P778 lapte UHT Pilos at Lidl previous week 10%
                new Discount() {{
                    setProductId("P778");
                    setProductName("lapte UHT");
                    setBrand("Pilos");
                    setStore("lidl");
                    setFromDate(LocalDate.of(2025, 4, 28));
                    setToDate(LocalDate.of(2025, 5, 3));
                    setPercentage(10);
                }}
                //  No discount for Ariel detergent
                // No discount for aspirator
        );

        LocalDate currentDate = LocalDate.of(2025, 5, 1);

        // Call service
        BasketOptimizationResultDTO result = service.optimizeBasketDetailed(shoppingList, productsByStore, allDiscounts, currentDate);

        // Print JSON output to see structure and values
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        String jsonResult = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        System.out.println(jsonResult);

        // Basic assertions
        assertNotNull(result);
        assertEquals(2, result.getStores().size()); // two stores should appear (kaufland and lidl)

        // Find the chosen milk product (lapte UHT brandless) - should be Lidl's Pilos 5.00 price with 10% discount applied
        Product milk = result.getStores().stream()
                .flatMap(store -> store.getProducts().stream())
                .filter(p -> p.getProductName().equalsIgnoreCase("lapte UHT"))
                .findFirst()
                .orElse(null);
        assertNotNull(milk);
        assertEquals("lidl", milk.getStore());
        assertEquals("Pilos", milk.getBrand());
        assertEquals(5.00, milk.getPrice());

        // Find the detergent Ariel product - should be from Kaufland without discount
        Product detergent = result.getStores().stream()
                .flatMap(store -> store.getProducts().stream())
                .filter(p -> p.getProductName().equalsIgnoreCase("detergent lichid") && "Ariel".equalsIgnoreCase(p.getBrand()))
                .findFirst()
                .orElse(null);
        assertNotNull(detergent);
        assertEquals("kaufland", detergent.getStore());

        // Check totalCost (should be discounted milk + detergent price)
        double expectedMilkPriceAfterDiscount = 5.00 * 0.9; // 10% discount from Lidl
        double expectedDetergentPrice = 50.50; // no discount on Ariel detergent
        double expectedTotalCost = expectedMilkPriceAfterDiscount + expectedDetergentPrice;
        assertEquals(expectedTotalCost, result.getTotalCost(), 0.01);

        // Check totalSavings: 10% off on Lidl milk, 5.00 - 4.50 = 0.5 savings
        double expectedSavings = 5.00 - expectedMilkPriceAfterDiscount;
        assertEquals(expectedSavings, result.getTotalSavings(), 0.01);

        // Check unmatchedItems contains aspirator (no products)
        assertEquals(1, result.getUnmatchedItems().size());
        assertEquals("aspirator", result.getUnmatchedItems().get(0).getProductName().toLowerCase());

        // Timestamp should be recent (within last 1 minute)
        assertTrue(result.getTimestamp().isAfter(LocalDateTime.now().minusMinutes(1)));
    }
}