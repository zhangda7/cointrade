package com.spare.cointrade.model;

import lombok.Data;

@Data
public class Balance {
    private CoinType coinType;

    private Double freeAmount;

    private Double lockedAmount;

    public Balance() {}

    public Balance(CoinType coinType, Double freeAmount, Double lockedAmount) {
        this.coinType = coinType;
        this.freeAmount = freeAmount;
        this.lockedAmount = lockedAmount;
    }
}
