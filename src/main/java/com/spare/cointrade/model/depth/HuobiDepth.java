package com.spare.cointrade.model.depth;

import lombok.Data;

/**
 * Created by dada on 2017/8/20.
 */
@Data
public class HuobiDepth {

    private String ch;

    private Long ts;

    private Tick tick;

}
