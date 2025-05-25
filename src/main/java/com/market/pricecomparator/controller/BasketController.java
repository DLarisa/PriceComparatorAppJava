package com.market.pricecomparator.controller;

import com.market.pricecomparator.dto.BasketOptimizationResultDTO;
import com.market.pricecomparator.dto.ShoppingItemDTO;
import com.market.pricecomparator.model.Discount;
import com.market.pricecomparator.model.Product;
import com.market.pricecomparator.service.BasketOptimizerService;
import com.market.pricecomparator.service.DiscountService;
import com.market.pricecomparator.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/basket")
public class BasketController {
    private final BasketOptimizerService basketOptimizerService;
    private final DiscountService discountService;
    private final ProductService productService;

    // Base directory path for discounts CSV files
    private final String discountsBaseDir = "src/main/resources/data";

    @Autowired
    public BasketController(BasketOptimizerService basketOptimizerService,
                            DiscountService discountService,
                            ProductService productService) {
        this.basketOptimizerService = basketOptimizerService;
        this.discountService = discountService;
        this.productService = productService;
    }

    @PostMapping("/optimize")
    public BasketOptimizationResultDTO optimizeBasket(@RequestBody List<ShoppingItemDTO> shoppingList) {
        // LocalDate currentDate = LocalDate.now(); --- currentDate normally
        LocalDate currentDate = LocalDate.of(2025, 5, 8);

        // Load products grouped by store
        Map<String, List<Product>> productsByStore = productService.loadProductsByStore(currentDate);

        // Load discounts for current and previous week
        List<String> stores = List.copyOf(productsByStore.keySet());
        List<Discount> allDiscounts = discountService.loadDiscountsForCurrentAndPreviousWeek(stores, currentDate, discountsBaseDir);

        // Call optimizer service
        return basketOptimizerService.optimizeBasketDetailed(shoppingList, productsByStore, allDiscounts, currentDate);
    }
}
