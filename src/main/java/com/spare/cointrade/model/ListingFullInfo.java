package com.spare.cointrade.model;

import lombok.Data;

import java.util.Comparator;

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

    private Long timestamp;

    private InfoStatus infoStatus;

    public ListingFullInfo() {
        this.buyDepth = new ListingDepth(new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                return o1.compareTo(o2);
            }
        });
        this.sellDepth = new ListingDepth(new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                return o2.compareTo(o1);
            }
        });
    }

    public String toKey() {
        StringBuilder sb = new StringBuilder(200);
        sb.append(tradePlatform.name() + "_");
        sb.append(tradeType.name() + "_");
        sb.append(sourceCoinType.name() + "_");
        sb.append(targetCoinType.name() + "_");
        return sb.toString();
    }
}
