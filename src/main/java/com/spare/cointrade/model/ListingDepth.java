package com.spare.cointrade.model;

import lombok.Data;

import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * 一个挂单深度信息
 * 比如买单或卖单
 */
@Data
public class ListingDepth {

    private ConcurrentSkipListMap<Double, DepthInfo> depthInfoMap;

    public ListingDepth() {
        this.depthInfoMap = new ConcurrentSkipListMap<>();
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
