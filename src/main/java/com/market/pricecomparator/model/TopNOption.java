package com.market.pricecomparator.model;

import lombok.Getter;

@Getter
public enum TopNOption {
    FIVE(5),
    TEN(10),
    FIFTEEN(15),
    TWENTY(20);

    private final int value;

    TopNOption(int value) {
        this.value = value;
    }
}
