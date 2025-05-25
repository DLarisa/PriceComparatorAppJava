package com.market.pricecomparator.service;

import com.market.pricecomparator.model.Product;
import com.market.pricecomparator.util.CsvLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

public class ProductServiceTest {
    private CsvLoader csvLoader;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        csvLoader = mock(CsvLoader.class);
        productService = new ProductService(csvLoader);
    }

    @Test
    void loadProductsByStore_shouldCallCsvLoaderForEachStoreAndReturnMap() {
        LocalDate testDate = LocalDate.of(2025, 5, 25);

        // Prepare dummy product lists for each store
        List<Product> lidlProducts = List.of(new Product());
        List<Product> kauflandProducts = List.of(new Product(), new Product());
        List<Product> profiProducts = List.of();

        // Stub the csvLoader responses for each expected file path
        when(csvLoader.loadProducts("src/main/resources/data/lidl_2025-05-25.csv")).thenReturn(lidlProducts);
        when(csvLoader.loadProducts("src/main/resources/data/kaufland_2025-05-25.csv")).thenReturn(kauflandProducts);
        when(csvLoader.loadProducts("src/main/resources/data/profi_2025-05-25.csv")).thenReturn(profiProducts);

        // Call the method under test
        Map<String, List<Product>> result = productService.loadProductsByStore(testDate);

        // Verify the returned map contains all stores and correct product lists
        assertEquals(3, result.size());
        assertSame(lidlProducts, result.get("lidl"));
        assertSame(kauflandProducts, result.get("kaufland"));
        assertSame(profiProducts, result.get("profi"));

        // Verify csvLoader.loadProducts was called exactly once per store with correct file paths
        verify(csvLoader).loadProducts("src/main/resources/data/lidl_2025-05-25.csv");
        verify(csvLoader).loadProducts("src/main/resources/data/kaufland_2025-05-25.csv");
        verify(csvLoader).loadProducts("src/main/resources/data/profi_2025-05-25.csv");
        verifyNoMoreInteractions(csvLoader);
    }
}
