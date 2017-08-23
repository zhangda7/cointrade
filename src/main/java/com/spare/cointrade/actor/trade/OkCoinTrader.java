package com.spare.cointrade.actor.trade;

import akka.actor.AbstractActor;
import akka.actor.Props;
import com.spare.cointrade.model.trade.HuobiTrade;
import com.spare.cointrade.model.trade.OkCoinTrade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 执行实际交易的actor
 * Created by dada on 2017/8/20.
 */
public class OkCoinTrader extends AbstractActor {

    private static Logger logger = LoggerFactory.getLogger(OkCoinTrader.class);

    public static Props props() {
        return Props.create(OkCoinTrader.class, () -> new OkCoinTrader());
    }

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder().match(OkCoinTrade.class, trade -> {
            logger.info("Receive ok coin trade {}", trade);
        }).build();
    }

}
