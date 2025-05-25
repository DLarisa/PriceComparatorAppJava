package com.market.pricecomparator.controller;

import com.market.pricecomparator.dto.ProductValueDTO;
import com.market.pricecomparator.service.ProductRecommendationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/recommendations")
public class ProductRecommendationController {

    private final ProductRecommendationService recommendationService;

    public ProductRecommendationController(ProductRecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    /**
     * Get product substitutes or recommendations by best value per unit.
     * Filters are optional.
     *
     * @param productName     required product name to find substitutes for
     * @param brand         optional brand filter
     * @param store         optional store filter
     * @param date          optional date, defaults to today if not specified
     */
    @GetMapping("/substitutes")
    public List<ProductValueDTO> getSubstitutes(
            @RequestParam String productName,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String store,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate queryDate = (date != null) ? date : LocalDate.now();

        // lowercase inputs to normalize filtering
        String normalizedProductName = productName.toLowerCase();
        Optional<String> normalizedBrand = Optional.ofNullable(brand).map(String::toLowerCase);
        Optional<String> normalizedStore = Optional.ofNullable(store).map(String::toLowerCase);

        return recommendationService.findBestValueProducts(
                normalizedProductName,
                normalizedBrand,
                normalizedStore,
                queryDate);
    }
}
