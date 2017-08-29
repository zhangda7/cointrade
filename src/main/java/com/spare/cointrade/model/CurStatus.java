package com.spare.cointrade.model;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.Date;

/**
 * Created by dada on 2017/8/29.
 */
@Data
public class CurStatus {

    @JSONField(format="yyyy-MM-dd HH:mm:ss")
    private Date huobiDate;

    @JSONField (format="yyyy-MM-dd HH:mm:ss")
    private Date okCoinDate;

    private Double delta1;

    private Double delta2;

}