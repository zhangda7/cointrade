package com.spare.cointrade.actor.consumer;

import akka.actor.AbstractActor;
import akka.actor.Props;
import com.spare.cointrade.model.HuobiDepth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 接收huobi网的原始信息
 * Created by dada on 2017/8/20.
 */
public class HuobiConsumer extends AbstractActor {

    private static Logger logger = LoggerFactory.getLogger(HuobiConsumer.class);

    public static Props props () {
        return Props.create(HuobiConsumer.class, () -> new HuobiConsumer());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(HuobiDepth.class, depth -> {
            onMessage(depth);
        }).build();
    }

    public void onMessage(HuobiDepth depth) {
//        logger.info("Receive {}", depth);
        //TODO write this message to mongo

    }
}
