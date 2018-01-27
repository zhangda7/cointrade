package com.spare.cointrade.model;

import lombok.Data;

import java.util.Map;

@Data
public class OpenExchangeData {

    private Long timestamp;

    private String base;

    private Map<String, Double> rates;
}
