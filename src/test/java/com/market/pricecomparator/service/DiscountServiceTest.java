package com.market.pricecomparator.service;

import com.market.pricecomparator.dto.BestDiscountsRequestDTO;
import com.market.pricecomparator.dto.ProductDiscountDTO;
import com.market.pricecomparator.model.Discount;
import com.market.pricecomparator.model.Product;
import com.market.pricecomparator.model.TopNOption;
import com.market.pricecomparator.util.CsvLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

public class DiscountServiceTest {
    private DiscountService discountService;
    private CsvLoader csvLoader;
    private ProductService productService;

    @BeforeEach
    void setup() {
        csvLoader = mock(CsvLoader.class);
        productService = mock(ProductService.class);
        discountService = spy(new DiscountService(csvLoader, productService));
    }

    @Test
    void testLoadDiscountsForCurrentAndPreviousWeek_returnsExpectedDiscounts() {
        String store = "lidl";
        LocalDate currentDate = LocalDate.of(2025, 5, 8); // current week
        LocalDate previousWeekDate = currentDate.minusDays(7); // previous week

        Discount currentDiscount = new Discount() {{
            setProductId("P123");
            setStore(store);
            setFromDate(currentDate.minusDays(0));
            setToDate(currentDate.plusDays(5));
            setPercentage(10);
        }};
        Discount previousDiscount = new Discount() {{
            setProductId("P123");
            setStore(store);
            setFromDate(previousWeekDate.minusDays(2));
            setToDate(previousWeekDate.plusDays(7));
            setPercentage(3);
        }};
        Discount outOfRangeDiscount = new Discount() {{
            setProductId("P123");
            setStore(store);
            setFromDate(previousWeekDate.minusDays(30));
            setToDate(previousWeekDate.minusDays(15));
            setPercentage(7);
        }};

        String currentFilePath = String.format("src/main/resources/data/%s_discounts_%s.csv", store, currentDate);
        String previousFilePath = String.format("src/main/resources/data/%s_discounts_%s.csv", store, previousWeekDate);

        when(csvLoader.loadDiscounts(currentFilePath)).thenReturn(List.of(currentDiscount, outOfRangeDiscount));
        when(csvLoader.loadDiscounts(previousFilePath)).thenReturn(List.of(previousDiscount));

        List<Discount> result = discountService.loadDiscountsForCurrentAndPreviousWeek(
                List.of(store), currentDate, "src/main/resources/data");

        assertEquals(2, result.size());
        assertTrue(result.contains(currentDiscount));
        assertTrue(result.contains(previousDiscount));
        assertFalse(result.contains(outOfRangeDiscount));
    }

    @Test
    void testLoadDiscountsForCurrentAndPreviousWeek_handlesEmptyFilesGracefully() {
        String store = "aldi";
        LocalDate currentDate = LocalDate.of(2025, 5, 8);
        String currentFilePath = String.format("src/main/resources/data/%s_discounts_%s.csv", store, currentDate);
        String previousFilePath = String.format("src/main/resources/data/%s_discounts_%s.csv", store, currentDate.minusDays(7));

        when(csvLoader.loadDiscounts(currentFilePath)).thenReturn(Collections.emptyList());
        when(csvLoader.loadDiscounts(previousFilePath)).thenReturn(Collections.emptyList());

        List<Discount> result = discountService.loadDiscountsForCurrentAndPreviousWeek(
                List.of(store), currentDate, "src/main/resources/data");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testLoadDiscountsForCurrentAndPreviousWeek_multipleStores() {
        LocalDate date = LocalDate.of(2025, 5, 8);
        Discount d1 = new Discount() {{
            setProductId("P123");
            setStore("lidl");
            setFromDate(date.minusDays(1));
            setToDate(date.plusDays(1));
            setPercentage(10);
        }};
        Discount d2 = new Discount() {{
            setProductId("P123");
            setStore("profi");
            setFromDate(date.minusDays(1));
            setToDate(date.plusDays(2));
            setPercentage(8);
        }};

        when(csvLoader.loadDiscounts(contains("lidl"))).thenReturn(List.of(d1));
        when(csvLoader.loadDiscounts(contains("profi"))).thenReturn(List.of(d2));

        List<Discount> result = discountService.loadDiscountsForCurrentAndPreviousWeek(
                List.of("lidl", "profi"), date, "src/main/resources/data");

        assertEquals(4, result.size());
        assertTrue(result.contains(d1));
        assertTrue(result.contains(d2));
    }

    @Test
    void testGetBestDiscounts_filtersByProductNameAndBrandAndLimitsTopN() {
        LocalDate date = LocalDate.of(2025, 5, 8);

        Product product1 = new Product();
        product1.setProductId("P1");
        product1.setProductName("Milk");
        product1.setBrand("BrandA");
        product1.setPrice(2.0);

        Product product2 = new Product();
        product2.setProductId("P2");
        product2.setProductName("Milk");
        product2.setBrand("BrandB");
        product2.setPrice(2.5);

        Product product3 = new Product();
        product3.setProductId("P3");
        product3.setProductName("Bread");
        product3.setBrand("BrandA");
        product3.setPrice(1.5);

        // Mock products by store
        Map<String, List<Product>> productsByStore = Map.of(
                "store1", List.of(product1, product2, product3)
        );
        when(productService.loadProductsByStore(date)).thenReturn(productsByStore);

        // Discounts (some matching productIds)
        Discount discount1 = new Discount();
        discount1.setProductId("P1");
        discount1.setStore("store1");
        discount1.setFromDate(date.minusDays(1));
        discount1.setToDate(date.plusDays(1));
        discount1.setPercentage(10);

        Discount discount2 = new Discount();
        discount2.setProductId("P2");
        discount2.setStore("store1");
        discount2.setFromDate(date.minusDays(1));
        discount2.setToDate(date.plusDays(1));
        discount2.setPercentage(40);

        Discount discount3 = new Discount();
        discount3.setProductId("P3");
        discount3.setStore("store1");
        discount3.setFromDate(date.minusDays(1));
        discount3.setToDate(date.plusDays(1));
        discount3.setPercentage(10);

        // Mock loadDiscountsForCurrentAndPreviousWeek to return discounts for store1
        when(discountService.loadDiscountsForCurrentAndPreviousWeek(List.of("store1"), date, "src/main/resources/data"))
                .thenReturn(List.of(discount1, discount2, discount3));

        // Filter: productName = "Milk", brand = "BrandA", topN = 1
        BestDiscountsRequestDTO filter = new BestDiscountsRequestDTO();
        filter.setProductName("Milk");
        filter.setBrand("");
        filter.setTopN(TopNOption.FIVE);

        List<ProductDiscountDTO> results = discountService.getBestDiscounts(date, filter);

        assertEquals(2, results.size());

        ProductDiscountDTO dto = results.get(0);
        assertEquals("P2", dto.getProductId());
        assertEquals("Milk", dto.getProductName());
        assertEquals("BrandB", dto.getBrand());
        assertEquals(40, dto.getDiscountPercentage());
        assertEquals("store1", dto.getStore());
    }

    @Test
    void testGetTopDiscountsForStore_success() {
        LocalDate date = LocalDate.of(2025, 5, 8);
        String store = "store1";

        Product product1 = new Product();
        product1.setProductId("P1");
        product1.setProductName("Milk");
        product1.setBrand("BrandA");
        product1.setPrice(2.0);

        List<Product> products = List.of(product1);

        when(productService.loadProductsByStore(date))
                .thenReturn(Map.of(store, products));

        Discount discount1 = new Discount();
        discount1.setProductId("P1");
        discount1.setStore(store);
        discount1.setFromDate(date.minusDays(1));
        discount1.setToDate(date.plusDays(1));
        discount1.setPercentage(25);

        doReturn(List.of(discount1)).when(discountService)
                .loadDiscountsForCurrentAndPreviousWeek(List.of(store), date, "src/main/resources/data");

        BestDiscountsRequestDTO filter = new BestDiscountsRequestDTO();
        filter.setTopN(TopNOption.FIVE);

        List<ProductDiscountDTO> result = discountService.getTopDiscountsForStore(date, store, filter);

        assertEquals(1, result.size());
        ProductDiscountDTO dto = result.get(0);
        assertEquals("P1", dto.getProductId());
        assertEquals(store, dto.getStore());
        assertEquals(25, dto.getDiscountPercentage());
    }

    @Test
    void testGetTopDiscountsForStore_noProducts_throwsException() {
        LocalDate date = LocalDate.of(2025, 5, 8);
        String store = "store1";

        when(productService.loadProductsByStore(date)).thenReturn(Map.of());

        BestDiscountsRequestDTO filter = new BestDiscountsRequestDTO();
        filter.setTopN(TopNOption.TEN);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> discountService.getTopDiscountsForStore(date, store, filter));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void testGetTopDiscountsAcrossStores_success() {
        LocalDate date = LocalDate.of(2025, 5, 8);

        Product product1 = new Product();
        product1.setProductId("P1");
        product1.setProductName("Milk");
        product1.setBrand("BrandA");
        product1.setPrice(2.0);

        Product product2 = new Product();
        product2.setProductId("P2");
        product2.setProductName("Bread");
        product2.setBrand("BrandB");
        product2.setPrice(1.5);

        Map<String, List<Product>> productsByStore = Map.of(
                "store1", List.of(product1),
                "store2", List.of(product2)
        );

        when(productService.loadProductsByStore(date)).thenReturn(productsByStore);

        Discount discount1 = new Discount();
        discount1.setProductId("P1");
        discount1.setStore("store1");
        discount1.setFromDate(date.minusDays(1));
        discount1.setToDate(date.plusDays(1));
        discount1.setPercentage(20);

        Discount discount2 = new Discount();
        discount2.setProductId("P2");
        discount2.setStore("store2");
        discount2.setFromDate(date.minusDays(1));
        discount2.setToDate(date.plusDays(1));
        discount2.setPercentage(15);

        doReturn(List.of(discount1)).when(discountService)
                .loadDiscountsForCurrentAndPreviousWeek(List.of("store1"), date, "src/main/resources/data");

        doReturn(List.of(discount2)).when(discountService)
                .loadDiscountsForCurrentAndPreviousWeek(List.of("store2"), date, "src/main/resources/data");

        BestDiscountsRequestDTO filter = new BestDiscountsRequestDTO();
        filter.setTopN(TopNOption.TEN);

        List<ProductDiscountDTO> results = discountService.getTopDiscountsAcrossStores(date, filter);

        assertEquals(2, results.size());

        // Verify the stores are present
        List<String> stores = results.stream().map(ProductDiscountDTO::getStore).distinct().collect(Collectors.toList());
        assertTrue(stores.contains("store1"));
        assertTrue(stores.contains("store2"));
    }


}
