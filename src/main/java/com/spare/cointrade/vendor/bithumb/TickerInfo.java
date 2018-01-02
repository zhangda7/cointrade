package com.spare.cointrade.vendor.bithumb;

import lombok.Data;

@Data
public class TickerInfo {

    private Double opening_price;

    private Double closeing_price;

    private Double buy_price;

    private Double sell_price;

    private Long date;

}
