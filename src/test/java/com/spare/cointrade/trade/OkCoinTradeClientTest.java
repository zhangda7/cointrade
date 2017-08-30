package com.spare.cointrade.trade;

import com.alibaba.fastjson.JSON;
import com.spare.cointrade.model.TradeAction;
import com.spare.cointrade.trade.okcoin.OkCoinTradeClient;
import com.spare.cointrade.trade.okcoin.OrderDetail;
import org.apache.http.HttpException;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by dada on 2017/8/29.
 */
public class OkCoinTradeClientTest {

    private static Logger logger = LoggerFactory.getLogger(OkCoinTradeClientTest.class);

    OkCoinTradeClient okCoinTradeClient;

    @Before
    public void before() throws IOException, HttpException {
        okCoinTradeClient = new OkCoinTradeClient("", "");
        okCoinTradeClient.init();
    }

    @Test
    public void testOrder() throws IOException, HttpException {
        String orderId = okCoinTradeClient.createEthOrder(1.0, 0.01, TradeAction.BUY);
        System.out.println(orderId);
    }

    @Test
    public void testQueryOrder() throws IOException, HttpException {
        OrderDetail orderDetail = okCoinTradeClient.queryOrder("63964045");
        System.out.println(JSON.toJSONString(orderDetail));
    }

}
