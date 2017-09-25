package com.spare.cointrade.controller;

import com.alibaba.fastjson.JSON;
import com.spare.cointrade.trade.huobi.HuobiTradeClient;
import com.spare.cointrade.trade.okcoin.OkCoinTradeClient;
import org.apache.http.HttpException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Created by dada on 2017/8/29.
 */
@RestController
public class UserAccountController {

    @Autowired
    HuobiTradeClient huobiTradeClient;

    @Autowired
    OkCoinTradeClient okCoinTradeClient;

    @RequestMapping("/user/huobi")
    public String huobiUser() {
        return JSON.toJSONString(huobiTradeClient.queryBalance());
    }

    @RequestMapping("/user/okcoin")
    public String okCoinUser() throws IOException, HttpException {
        return okCoinTradeClient.queryUser();
    }

}
