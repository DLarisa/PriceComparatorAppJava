package com.market.pricecomparator.controller;

import com.market.pricecomparator.dto.BestDiscountsRequestDTO;
import com.market.pricecomparator.dto.ProductDiscountDTO;
import com.market.pricecomparator.model.TopNOption;
import com.market.pricecomparator.service.DiscountService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/discounts")
public class DiscountController {
    private final DiscountService discountService;

    public DiscountController(DiscountService discountService) {
        this.discountService = discountService;
    }

    @PostMapping("/top")
    public List<ProductDiscountDTO> getBestDiscounts(@RequestBody BestDiscountsRequestDTO filter) {
        if (filter.getProductName() == null || filter.getProductName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productName is required");
        }

        if (filter.getTopN() == null) {
            filter.setTopN(TopNOption.FIVE); // default
        }

        LocalDate date = (filter.getDate() != null) ? filter.getDate() : LocalDate.now();

        return discountService.getBestDiscounts(date, filter);
    }

    @PostMapping("/top-store")
    public List<ProductDiscountDTO> getTopDiscountsForStore(@RequestBody BestDiscountsRequestDTO request) {
        if (request.getStore() == null || request.getStore().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "store is required");
        }

        if (request.getTopN() == null) {
            request.setTopN(TopNOption.FIVE);
        }

        LocalDate date = (request.getDate() != null) ? request.getDate() : LocalDate.now();

        return discountService.getTopDiscountsForStore(date, request.getStore(), request);
    }

    @PostMapping("/top-all")
    public List<ProductDiscountDTO> getTopDiscountsAcrossAllStores(@RequestBody BestDiscountsRequestDTO request) {
        if (request.getTopN() == null) {
            request.setTopN(TopNOption.FIVE);
        }

        LocalDate date = (request.getDate() != null) ? request.getDate() : LocalDate.now();

        return discountService.getTopDiscountsAcrossStores(date, request);
    }

    @PostMapping("/new")
    public List<ProductDiscountDTO> getNewDiscounts(@RequestBody BestDiscountsRequestDTO filter) {
        LocalDate date = (filter.getDate() != null) ? filter.getDate() : LocalDate.now();

        if (filter.getTopN() == null || filter.getTopN().getValue() <= 0) {
            filter.setTopN(TopNOption.FIVE);
        }

        // Validate newWithinDays if provided
        if (filter.getNewWithinDays() != null &&
                (filter.getNewWithinDays() < 1 || filter.getNewWithinDays() > 14)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "newWithinDays must be between 1 and 14");
        }

        return discountService.getNewDiscounts(date, filter);
    }
}
