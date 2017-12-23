package com.spare.cointrade.model;

import lombok.Data;

/**
 * Created by dada on 2017/8/24.
 */
@Deprecated
@Data
public class TradeDepth {

    TradeSource source;

    /**
     * BTC, ETH...
     */
    private String type;

    private Double price;

    private Double amount;

}
