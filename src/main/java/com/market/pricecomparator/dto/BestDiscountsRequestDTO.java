package com.market.pricecomparator.dto;

import com.market.pricecomparator.model.TopNOption;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BestDiscountsRequestDTO extends ShoppingItemDTO {
    private String store;
    private TopNOption topN = TopNOption.FIVE;        // default top 5
    private LocalDate date; // optional; defaults to now
    private Integer newWithinDays; // Optional: number of days to consider new, 1-14 days; defaults to 1
}
