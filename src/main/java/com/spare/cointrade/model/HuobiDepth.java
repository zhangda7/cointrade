package com.spare.cointrade.model;

import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.DeflaterOutputStream;

/**
 * Created by dada on 2017/8/20.
 */
@Data
public class HuobiDepth {

    private String ch;

    private Long ts;

    private Tick tick;

}
