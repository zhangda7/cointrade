package com.spare.cointrade.trade.okcoin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Created by dada on 2017/8/25.
 */
@Data
public class OrderDetail {

    private Double amount;

    private Double avg_price;

    private Long create_date;

    private Double deal_amount;

    private Long order_id;

    private Double price;

    private Integer status;

    private String symbol;

    private String type;

}