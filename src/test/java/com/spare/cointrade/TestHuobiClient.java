package com.spare.cointrade;

import com.spare.cointrade.realtime.huobi.HuobiClient;
import org.junit.Test;

/**
 * Created by dada on 2017/8/20.
 */
public class TestHuobiClient {

    @Test
    public void testStart() throws InterruptedException {
        HuobiClient huobiClient = new HuobiClient();
        huobiClient.startFetch();

        Thread.currentThread().join();
    }

}
