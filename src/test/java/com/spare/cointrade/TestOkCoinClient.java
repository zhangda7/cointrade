package com.spare.cointrade;

import com.spare.cointrade.realtime.huobi.HuobiClient;
import com.spare.cointrade.realtime.okcoin.OkcoinClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
        okcoinClient.startFetch();

        Thread.currentThread().join();
    }

}
