package com.spare.cointrade.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TradePair {

    /**
     * 唯一标识一次交易对
     */
    private String pairId;

    /**
     * 交易的方向，即正向(盈利)，或者反向（亏损）
     */
    private TradeDirection tradeDirection;

    private SignalTrade tradePair_1;

    private SignalTrade tradePair_2;

    /**
     * 对于binance的平台，如果交易的对标币种是BTC，需要一次额外的交易，去补平BTC的数量
     */
    private SignalTrade tradePair_3;
//    private List<SignalTrade> signalTradeList;

//    public TradePair() {
//        this.signalTradeList = new ArrayList<>();
//    }

}
