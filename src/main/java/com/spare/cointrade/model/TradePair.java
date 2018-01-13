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

//    private List<SignalTrade> signalTradeList;

//    public TradePair() {
//        this.signalTradeList = new ArrayList<>();
//    }

}
