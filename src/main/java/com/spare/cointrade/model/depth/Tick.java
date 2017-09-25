package com.spare.cointrade.model.depth;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Created by dada on 2017/8/20.
 */
@Data
public class Tick {

    private Long id;

    private Long ts;

    /**
     * 卖出价格
     */
    private List<List<Double>> bids;

    /**
     * 买入价格
     */
    private List<List<Double>> asks;

}
