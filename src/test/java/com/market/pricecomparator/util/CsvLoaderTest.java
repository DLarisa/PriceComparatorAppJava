package com.market.pricecomparator.util;

import com.market.pricecomparator.model.Discount;
import com.market.pricecomparator.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class CsvLoaderTest {
    private CsvLoader csvLoader;

    @BeforeEach
    void setup() {
        csvLoader = new CsvLoader();
    }

    @Test
    void testLoadProducts() {
        String path = Paths.get("src\\main\\resources\\data\\lidl_2025-05-01.csv").toString();

        List<Product> products = csvLoader.loadProducts(path);

        assertFalse(products.isEmpty());
        assertEquals(20, products.size(), "Number of loaded products does not match!");
        assertEquals("P001", products.get(0).getProductId());
        assertEquals("lidl", products.get(0).getStore());
        assertEquals(java.time.LocalDate.of(2025, 5, 1), products.get(0).getDate());
    }

    @Test
    void testLoadDiscounts() {
        String path = Paths.get("src\\main\\resources\\data\\lidl_discounts_2025-05-01.csv").toString();

        List<Discount> discounts = csvLoader.loadDiscounts(path);

        assertFalse(discounts.isEmpty());
        assertEquals(20, discounts.size(), "Number of loaded discounts does not match!");
        assertEquals("P001", discounts.get(0).getProductId());
        assertEquals("lidl", discounts.get(0).getStore());
        assertEquals(10, discounts.get(0).getPercentage());

        /*
        // Print all discounts for inspection
        System.out.println("Loaded Discounts:");
        discounts.forEach(System.out::println);
       */
    }
}
