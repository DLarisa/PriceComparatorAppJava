package com.market.pricecomparator.service;

import com.market.pricecomparator.model.Product;
import com.market.pricecomparator.util.CsvLoader;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductService {
    private final CsvLoader csvLoader;

    // Base directory path for product CSV files
    private final String productsBaseDir = "src/main/resources/data";

    // Stores list
    private final List<String> stores = List.of("lidl", "kaufland", "profi");

    public ProductService(CsvLoader csvLoader) {
        this.csvLoader = csvLoader;
    }

    /**
     * Loads products for the given stores and currentDate from CSV files.
     *
     * @param currentDate the date used to determine which CSV file to load
     * @return Map with key = store name, value = list of products for that store
     */
    public Map<String, List<Product>> loadProductsByStore(LocalDate currentDate) {
        return stores.stream()
                .collect(Collectors.toMap(
                        store -> store,
                        store -> {
                            String filePath = String.format("%s/%s_%s.csv", productsBaseDir, store, currentDate);
                            return csvLoader.loadProducts(filePath);
                        }
                ));
    }
}
