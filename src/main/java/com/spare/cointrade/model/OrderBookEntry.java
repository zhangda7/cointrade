package com.spare.cointrade.model;

import lombok.Data;

@Data
public class OrderBookEntry {

    private CoinType coinType;

    private Double price;

    private Double amount;

    /**
     * 价格高的平台
     */
    private TradePlatform platform1;

    /**
     * 价格低的平台
     */
    private TradePlatform platform2;

    /**
     * 平台间差价
     */
    private Double delta;

    /**
     * 归一化到 1 万元的差价
     */
    private Double normaliseDelta;

    public String toKey() {
        return platform1 + "_" + platform2 + "_" + coinType;
    }
}
