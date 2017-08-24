package com.spare.cointrade.model;

import lombok.Data;

/**
 * Created by dada on 2017/8/24.
 */
@Data
public class TradeDepth {

    /**
     * BTC, ETH...
     */
    private String type;

    private Double price;

    private Double amount;

}
