package com.market.pricecomparator.service;

import com.market.pricecomparator.dto.PricePointDTO;
import com.market.pricecomparator.model.Product;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PriceHistoryService {

    private final ProductService productService;

    public PriceHistoryService(ProductService productService) {
        this.productService = productService;
    }

    public List<PricePointDTO> getPriceHistory(
            Optional<String> productName,
            Optional<String> brand,
            Optional<String> store,
            Optional<String> category,
            LocalDate startDate,
            LocalDate endDate) {

        List<LocalDate> dates = getDatesBetween(startDate, endDate);

        List<PricePointDTO> history = new ArrayList<>();

        for (LocalDate date : dates) {
            Map<String, List<Product>> productsByStore = productService.loadProductsByStore(date);

            // flatten all products
            List<Product> allProducts = productsByStore.values().stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

            // filter products by optional params
            List<Product> filtered = allProducts.stream()
                    .filter(p -> productName.map(name -> p.getProductName().equalsIgnoreCase(name)).orElse(true))
                    .filter(p -> brand.map(b -> p.getBrand().equalsIgnoreCase(b)).orElse(true))
                    .filter(p -> store.map(s -> p.getStore().equalsIgnoreCase(s)).orElse(true))
                    .filter(p -> category.map(c -> p.getCategory().equalsIgnoreCase(c)).orElse(true))
                    .collect(Collectors.toList());

            if (filtered.isEmpty()) continue;

            // average price for the day
            double avgPrice = filtered.stream()
                    .mapToDouble(Product::getPrice)
                    .average()
                    .orElse(0);

            history.add(new PricePointDTO(date, avgPrice, filtered.size()));
        }

        return history;
    }

    private List<LocalDate> getDatesBetween(LocalDate start, LocalDate end) {
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            dates.add(date);
        }
        return dates;
    }
}