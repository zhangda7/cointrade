package com.spare.cointrade.realtime.huobi;

import com.spare.cointrade.model.CoinType;
import com.spare.cointrade.model.TradeType;
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


//    @PostConstruct
    public void startFetch(String topic, CoinType sourceType, CoinType targetType) {
        try {
            WebSocketUtils.closeQuietly();
            WebSocketUtils.executeWebSocket(TradeType.COIN_COIN, topic, sourceType, targetType);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PostConstruct
    public void tempStart() {
        startFetch("market.ethbtc.depth.step0", CoinType.BTC, CoinType.ETH);
    }

}
