package com.spare.cointrade.realtime.huobi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by dada on 2017/8/20.
 */
@Component
public class HuobiClient {

    private static Logger logger = LoggerFactory.getLogger(HuobiClient.class);


    @PostConstruct
    public void startFetch() {
        try {
            WebSocketUtils.executeWebSocket();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
