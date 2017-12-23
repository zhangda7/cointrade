package com.spare.cointrade.model;

import lombok.Data;

/**
 * Created by dada on 2017/8/30.
 */
@Deprecated
@Data
public class AccountInfo {

    /**
     * huobi or okcoin
     */
    private String source;

    /**
     * btc, etc, eth...
     */
    private String symbol;

    /**
     * cash
     */
    private Double money;

    /**
     * coin amount
     */
    private Double coinAmount;

    private String state;

    private String id;

    private String type;

}
