package com.spare.cointrade.actor.trade;

import akka.actor.AbstractActor;
import akka.actor.Props;
import com.spare.cointrade.model.trade.HuobiTrade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 执行实际交易的actor
 * Created by dada on 2017/8/20.
 */
public class HuobiTrader extends AbstractActor {

    private static Logger logger = LoggerFactory.getLogger(HuobiTrader.class);

    public static Props props() {
        return Props.create(HuobiTrader.class, () -> new HuobiTrader());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(HuobiTrade.class, trade -> {
            logger.info("Receive huobi trade {}", trade);
        }).build();
    }
}
