package com.spare.cointrade.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TradePair {

    private List<SignalTrade> signalTradeList;

    public TradePair() {
        this.signalTradeList = new ArrayList<>();
    }

}
