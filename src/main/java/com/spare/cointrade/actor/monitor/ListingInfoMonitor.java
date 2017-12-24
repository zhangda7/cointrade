package com.spare.cointrade.actor.monitor;

import akka.actor.AbstractActor;
import akka.actor.Props;
import com.spare.cointrade.actor.trade.TradeJudgeV2;
import com.spare.cointrade.model.ListingFullInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 实时汇总接收到的信息
 * 后面可能改名字，该类收到消息，汇总结束后，再发给后面的trade模块进行交易
 * Created by dada on 2017/12/24.
 */
public class ListingInfoMonitor extends AbstractActor {

    private static Logger logger = LoggerFactory.getLogger(ListingInfoMonitor.class);

    public static Props props () {
        return Props.create(ListingInfoMonitor.class, () -> new ListingInfoMonitor());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(ListingFullInfo.class, (listingFullInfo -> {
            try {
                logger.info("Receive {}", listingFullInfo);
//                judgeClearCache();
//                parseHuobi(depth);
            } catch (Exception e) {
                logger.error("ERROR ", e);
            }
        })).build();
    }

}
