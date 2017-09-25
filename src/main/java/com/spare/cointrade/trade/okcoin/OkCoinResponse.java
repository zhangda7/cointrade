package com.spare.cointrade.trade.okcoin;

import lombok.Data;

/**
 * Created by dada on 2017/8/26.
 */
@Data
public class OkCoinResponse {

    private Boolean result;

    private Integer error_code;

    private Object data;

}
