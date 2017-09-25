package com.spare.cointrade.trade;

import com.alibaba.fastjson.JSON;
import com.spare.cointrade.model.HuobiAccount;
import com.spare.cointrade.model.TradeAction;
import com.spare.cointrade.trade.huobi.HuobiTradeClient;
import com.spare.cointrade.trade.huobi.OrderDetail;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Created by dada on 2017/8/22.
 */
public class HuobiTradeClientTest {

    HuobiTradeClient huobiTradeClient;

    @Before
    public void before() throws IllegalAccessException {
        huobiTradeClient = new HuobiTradeClient("","");
        huobiTradeClient.init();
    }

    @Test
    public void testCreateOrder() {
        String orderId = huobiTradeClient.createEthOrder(11.0, 23.0, TradeAction.SELL);
        System.out.println(orderId);
    }

    @Test
    public void testQueryOrder() {
        OrderDetail orderDetail = huobiTradeClient.queryOrder("33768848");
        System.out.println(JSON.toJSONString(orderDetail));
    }

    @Test
    public void testQueryUser() {
        HuobiAccount huobiAccount = huobiTradeClient.queryBalance();
        System.out.println(JSON.toJSONString(huobiAccount));
    }

}
