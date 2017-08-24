package com.spare.cointrade.actor.minitor;

import akka.actor.AbstractActor;
import akka.actor.Props;
import com.spare.cointrade.actor.consumer.HuobiConsumer;
import com.spare.cointrade.dao.TradeEventMongoDao;
import com.spare.cointrade.util.ApplicationContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 监控已经提交的订单状态，并取消失败的订单
 * Created by dada on 2017/8/25.
 */
public class HuobiTradeMonitor extends AbstractActor {

    private static Logger logger = LoggerFactory.getLogger(HuobiTradeMonitor.class);

    public static Props props () {
        return Props.create(HuobiTradeMonitor.class, () -> new HuobiTradeMonitor());
    }


    private TradeEventMongoDao tradeEventMongoDao;

    public HuobiTradeMonitor() {
        tradeEventMongoDao = ApplicationContextHolder.getBean(TradeEventMongoDao.class);
    }

    @Override
    public Receive createReceive() {
        return null;
    }
}
