package com.spare.cointrade.model.trade;

import com.spare.cointrade.model.TradeAction;
import lombok.Data;

/**
 * Created by dada on 2017/8/23.
 */
@Data
public class OkCoinTrade {

    private TradeAction action;

    private String symbol;

    private Double price;

    private Double amount;

}
