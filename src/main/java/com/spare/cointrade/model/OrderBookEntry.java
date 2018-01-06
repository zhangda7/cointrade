package com.spare.cointrade.model;

import lombok.Data;

@Data
public class OrderBookEntry {

    private CoinType coinType;

    private Double price;

    private Double amount;

    /**
     * 平台间差价
     */
    private Double delta;

    /**
     * 归一化到 1 万元的差价
     */
    private Double normaliseDelta;
}
