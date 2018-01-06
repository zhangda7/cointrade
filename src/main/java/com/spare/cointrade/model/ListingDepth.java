package com.spare.cointrade.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * 一个挂单深度信息
 * 比如买单或卖单
 */
@Data
public class ListingDepth {

    private ConcurrentSkipListMap<Double, DepthInfo> depthInfoMap;

    public ListingDepth(Comparator<Double> comparator) {
        this.depthInfoMap = new ConcurrentSkipListMap<>(comparator);
    }

    @Data
    public class DepthInfo {

        /**
         * 原始的price信息
         * 如果是币币交易的，则是币币交易的price
         * 是未经修改的原始价格信息，使用时需要进行转换的
         */
        private Double oriPrice;

        /**
         * 归一化的价格信息
         * 目前是所有的价格都归一化到CNY计算
         */
        private Double normalizePrice;

        private Double amount;

        /**
         * 总的归一化价格
         * normalizePrice * amount
         */
        private Double totalNormalizePrice;

        /**
         * 累计？？？
         */
        private Double sumAmount;

    }

}
