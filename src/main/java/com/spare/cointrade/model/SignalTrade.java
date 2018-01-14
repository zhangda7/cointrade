package com.spare.cointrade.model;

import lombok.Data;

@Data
public class SignalTrade {

    private TradePlatform tradePlatform;

    /**
     * 要交易的币种是什么，比如ETH, QTUM
     */
    private CoinType sourceCoin;

    /**
     * 用哪个币种进行这个交易，比如KRW, USDT, BTC
     */
    private CoinType targetCoin;

    private TradeAction tradeAction;

    private Double price;

    private Double normalizePrice;

    private Double amount;

    private TradeResult result;

}
