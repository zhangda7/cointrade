package com.spare.cointrade.model;

import lombok.Data;

@Data
public class SignalTrade {

    private TradePlatform tradePlatform;

    private CoinType sourceCoin;

    private CoinType targetCoin;

    private TradeAction tradeAction;

    private Double price;

    private Double normalizePrice;

    private Double amount;

}
