package com.spare.cointrade.model;

import lombok.Data;

@Data
public class OrderBookHistory {

    private CoinType coinType;

    /**
     * 该币种总的盈利
     */
    private double totalProfit;

    /**
     * 该币种总的交易数量
     */
    private double totalAmount;

    /**
     * 该币种的平均盈利
     */
    private double averageProfit;

    private long updateTs;

}