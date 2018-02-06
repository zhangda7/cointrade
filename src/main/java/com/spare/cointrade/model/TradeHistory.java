package com.spare.cointrade.model;

import lombok.Data;

@Data
public class TradeHistory {

    private Long id;

    private TradePlatform tradePlatform;

    private TradeAction tradeAction;

    private String pairId;

    private CoinType coinType;

    private CoinType targetCoinType;

    private Double price;

    private Double normalizePrice;

    private Double normalizeFee;

    private Double amount;

    private String direction;

    private TradeResult result;

    /**
     * 本次盈利的数额，正数是盈利，负数是亏损
     */
    private Double profit;

    private String accountName;

    private Double preAccountSourceAmount;

    private Double afterAccountSourceAmount;

    private Double preAccountTargetAmount;

    private Double afterAccountTargetAmount;

    private String comment;

    private String gmtCreated;

    private String gmtModified;

}
