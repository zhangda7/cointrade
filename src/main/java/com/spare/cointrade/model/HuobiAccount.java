package com.spare.cointrade.model;

import lombok.Data;

import java.util.List;

/**
 * Created by dada on 2017/8/29.
 */
@Data
public class HuobiAccount {

    private Long id;

    private String state;

    private String type;

    private List<HuobiSubAccount> list;

}
