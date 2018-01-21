package com.spare.cointrade.model;

/**
 * Created by dada on 2017/8/23.
 */
public enum  TradeAction {

    BUY("BUY"),
    SELL("SELL"),
    HOLD("HOLD"),
    FAIL("FAIL"),
    WITH_DRAW("WITH_DRAW");

    private String value;

    TradeAction(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

}
