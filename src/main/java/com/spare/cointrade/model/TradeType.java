package com.spare.cointrade.model;

/**
 * 交易类型
 */
public enum TradeType {

    /**
     * 合约交易
     */
    FUTURE_USED,

    /**
     * 币币交易
     */
    COIN_COIN;

//    /**
//     * 源是ETH
//     * 使用BTC交易
//     */
//    ETH_BTC("ETH", "BTC"),
//
//    /**
//     * 源是ETH
//     * 使用CNY交易
//     */
//    ETH_CNY("ETH", "CNY");
//
//    /**
//     * source
//     */
//    private String source;
//
//    private String target;
//
//    TradeType(String source, String target) {
//        this.source = source;
//        this.target = target;
//    }
}
