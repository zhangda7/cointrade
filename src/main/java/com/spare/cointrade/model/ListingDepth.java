package com.spare.cointrade.model;

import lombok.Data;

import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentSkipListMap;

@Data
public class ListingDepth {

    private ConcurrentSkipListMap<Double, DepthInfo> depthInfoMap;

    @Data
    class DepthInfo {
        private Double price;

        private Double amount;

        /**
         * 累计？？？
         */
        private Double sumAmount;

    }

}
