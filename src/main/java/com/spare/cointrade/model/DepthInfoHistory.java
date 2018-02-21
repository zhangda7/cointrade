package com.spare.cointrade.model;

import lombok.Data;

import java.util.Date;

/**
 * 市场深度信息的保存
 */
@Data
public class DepthInfoHistory {

    private TradePlatform platform;

    private CoinType sourceCoin;

    private CoinType targetCoin;

    /**
     * 原始的price信息
     * 如果是币币交易的，则是币币交易的price
     * 是未经修改的原始价格信息，使用时需要进行转换的
     * 买1信息
     */
    private Double oriBidPrice1;

    /**
     * 归一化的价格信息
     * 目前是所有的价格都归一化到USD计算
     * 买1信息
     */
    private Double normalizeBidPrice1;

    /**
     * 买1的数量
     */
    private Double bidAmount1;

    /**
     * 卖1的信息
     */
    private Double oriAskPrice1;

    /**
     * 卖1的归一化价格
     */
    private Double normalizeAskPrice1;

    /**
     * 卖1的数量
     */
    private Double askAmount1;

    private Long sampleTs;

    private Date gmtCreated;

    private Date gmtModified;

}
