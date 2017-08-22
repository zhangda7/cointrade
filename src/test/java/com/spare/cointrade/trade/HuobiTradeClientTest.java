package com.spare.cointrade.trade;

import com.spare.cointrade.trade.huobi.HuobiTradeClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Created by dada on 2017/8/22.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class HuobiTradeClientTest {

    @Autowired
    HuobiTradeClient huobiTradeClient;

    @Test
    public void testCreateOrder() {
        huobiTradeClient.createEtcOrder(11.0, 23.0);
    }

    @Test
    public void testQueryUser() {

    }

}
