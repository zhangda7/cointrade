package com.spare.cointrade.model;

import lombok.Data;

@Data
public class TradeHistory {

    private Long id;

    private TradePlatform tradePlatform;

    private TradeAction tradeAction;

    private CoinType coinType;

    private CoinType targetCoinType;

    private Double price;

    private Double normalizePrice;

    private Double amount;

    private TradeResult result;

    private String accountName;

    private Double preAccountSourceAmount;

    private Double afterAccountSourceAmount;

    private Double preAccountTargetAmount;

    private Double afterAccountTargetAmount;

    private String comment;

    private String gmtCreated;

    private String gmtModified;

}
