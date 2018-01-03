package com.spare.cointrade.vendor.bithumb;

import lombok.Data;

import java.util.List;

@Data
public class OrderBookInfo {

    private Long timestamp;

    private String payment_currency;

    private String order_currency;

    private List<ListingPair> bids;

    private List<ListingPair> asks;

    @Data
    public class ListingPair {
        private Double quantity;

        private Double price;
    }
}
