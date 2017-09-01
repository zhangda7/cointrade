package com.spare.cointrade.model.depth;

import lombok.Data;

import java.util.List;

/**
 * Created by dada on 2017/8/20.
 */
@Data
public class OkcoinDepth {

    /**
     * 是否要清空原有数据，适用于断开重连
     */
    boolean clear;

    private List<List<String>> asks;

    private List<List<String>> bids;

    private Long timestamp;

}
