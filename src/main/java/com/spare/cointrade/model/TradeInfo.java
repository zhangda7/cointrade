package com.spare.cointrade.model;

import lombok.Data;

/**
 * Created by dada on 2017/9/3.
 */
@Deprecated
@Data
public class TradeInfo {

    private TradeSource source;

    private TradeAction action;

    private String symbol;

    private Double price;

    private Double amount;

    private String orderId;

    private String orderUrl;

    private Long ts;

    private String comment;

}
