package com.spare.cointrade.model;

import lombok.Data;

import java.util.Map;

@Data
public class Account {

    private TradePlatform tradePlatform;

    private String accountName;

    private Boolean canTrade;

    private Boolean canWithdraw;

    private Boolean canDeposite;

    private Long updateTs;

    private Map<CoinType, Balance> balanceMap;

}
