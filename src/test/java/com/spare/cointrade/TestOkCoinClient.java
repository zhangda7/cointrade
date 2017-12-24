package com.spare.cointrade;

import com.spare.cointrade.model.Pair;
import com.spare.cointrade.model.TradeType;
import com.spare.cointrade.realtime.huobi.HuobiClient;
import com.spare.cointrade.realtime.okcoin.BuissnesWebSocketServiceImpl;
import com.spare.cointrade.realtime.okcoin.OkcoinClient;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Created by dada on 2017/8/20.
 */
@RunWith(SpringRunner.class)
//@SpringBootTest
public class TestOkCoinClient {

//    @Autowired
    OkcoinClient okcoinClient;

    @Test
    public void testStart() throws InterruptedException {
        okcoinClient = new OkcoinClient();
        okcoinClient.startFetch(TradeType.COIN_COIN);

        Thread.currentThread().join();
    }

    @Test
    public void testPasre() {
        BuissnesWebSocketServiceImpl ws = new BuissnesWebSocketServiceImpl(TradeType.COIN_COIN);
        Pair<String> pair = ws.parseCoinSource("ok_sub_spot_bch_btc_depth");
        Assert.assertEquals(pair.get_1(), "bch");
        Assert.assertEquals(pair.get_2(), "btc");
    }

}
