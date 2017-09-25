package com.spare.cointrade.model.depth;

import lombok.Data;

/**
 * Created by dada on 2017/8/20.
 */
@Data
public class HuobiDepth {

    /**
     * 是否要清空原有数据，适用于断开重连
     */
    boolean clear;

    private String ch;

    private Long ts;

    private Tick tick;

}
