package com.spare.cointrade.model;

import lombok.Data;

/**
 * 挂牌信息
 * 精确到一个平台的、一对交易币种
 */
@Data
public class ListingFullInfo {

    private TradePlatform tradePlatform;

    private TradeType tradeType;

    private CoinType sourceCoinType;

    private CoinType targetCoinType;

    /**
     * 挂牌的买方价格
     */
    private ListingDepth buyDepth;

    /**
     * 挂牌的卖方价格
     */
    private ListingDepth sellDepth;

}
