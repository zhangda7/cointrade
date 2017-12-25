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
        private Double price;

        private Double amount;

        /**
         * 累计？？？
         */
        private Double sumAmount;

    }

}
