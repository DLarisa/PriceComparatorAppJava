package com.market.pricecomparator.controller;

import com.market.pricecomparator.dto.PricePointDTO;
import com.market.pricecomparator.service.PriceHistoryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/price-history")
public class PriceHistoryController {

    private final PriceHistoryService priceHistoryService;

    public PriceHistoryController(PriceHistoryService priceHistoryService) {
        this.priceHistoryService = priceHistoryService;
    }

    @GetMapping
    public List<PricePointDTO> getPriceHistory(
            @RequestParam Optional<String> productName,
            @RequestParam Optional<String> brand,
            @RequestParam Optional<String> store,
            @RequestParam Optional<String> category,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        LocalDate effectiveEndDate = endDate != null ? endDate : startDate.plusWeeks(2);

        return priceHistoryService.getPriceHistory(
                productName.map(String::toLowerCase),
                brand.map(String::toLowerCase),
                store.map(String::toLowerCase),
                category.map(String::toLowerCase),
                startDate,
                effectiveEndDate
        );
    }
}
