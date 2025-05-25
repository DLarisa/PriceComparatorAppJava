package com.market.pricecomparator.controller;

import com.market.pricecomparator.dto.PriceAlertMatchDTO;
import com.market.pricecomparator.service.PriceAlertService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/alerts")
public class PriceAlertController {

    private final PriceAlertService priceAlertService;

    public PriceAlertController(PriceAlertService priceAlertService) {
        this.priceAlertService = priceAlertService;
    }

    /**
     * Check if any product currently (or recently) has an effective price <= target.
     */
    @GetMapping("/price")
    public List<PriceAlertMatchDTO> getPriceAlerts(
            @RequestParam String productName,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String store,
            @RequestParam double targetPrice,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate queryDate = (date != null) ? date : LocalDate.now();

        return priceAlertService.checkPriceAgainstTarget(
                productName.trim().toLowerCase(),
                Optional.ofNullable(brand).map(String::trim).map(String::toLowerCase),
                Optional.ofNullable(store).map(String::trim).map(String::toLowerCase),
                targetPrice,
                queryDate
        );
    }
}
