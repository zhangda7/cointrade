package com.spare.cointrade.trade.huobi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Created by dada on 2017/8/25.
 */
@Data
public class OrderDetail {

    private Long id;

    @JsonProperty("account-id")
    private Long account_id;

    private String amount;

    @JsonProperty("created-at")
    private Long created_at;

    @JsonProperty("field-amount")
    private String field_amount;

    @JsonProperty("field-cash-amount")
    private String field_cash_amount;

    @JsonProperty("field-fees")
    private String field_fees;

    private String price;

    private String source;

    private String state;

    private String symbol;

    private String type;
}
