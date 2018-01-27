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
     * platform 1 的目标交易币种
     */
    private CoinType targetCoinType1;

    /**
     * 价格低的平台
     */
    private TradePlatform platform2;

    /**
     * platform 2 的目标交易币种
     */
    private CoinType targetCoinType2;

    /**
     * 平台间差价
     */
    private Double delta;

    /**
     * 归一化到 1 万元的差价
     */
    private Double normaliseDelta;

    public String toKey() {
        return platform1 + "_" + targetCoinType1 + "_" + platform2 + "_" + targetCoinType2 + "_" + coinType;
    }
}
