package com.spare.cointrade.actor.monitor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.spare.cointrade.dao.TradeEventMongoDao;
import com.spare.cointrade.model.TradeAction;
import com.spare.cointrade.model.trade.HuobiTrade;
import com.spare.cointrade.trade.huobi.HuobiTradeClient;
import com.spare.cointrade.trade.huobi.OrderDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.*;

/**
 * 监控已经提交的订单状态，并取消失败的订单
 * Created by dada on 2017/8/25.
 */
//@Component
//@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
//@Qualifier("huobiTradeMonitor")
//@Component
public class HuobiTradeMonitor{

    private static Logger logger = LoggerFactory.getLogger(HuobiTradeMonitor.class);

    @Autowired
    private TradeEventMongoDao tradeEventMongoDao;

    @Autowired
    private HuobiTradeClient huobiTradeClient;

    private static ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
            .setNameFormat("huobi-monitor-").build();

    private static ExecutorService executorService = Executors.newSingleThreadExecutor(namedThreadFactory);

    private static BlockingQueue<HuobiTrade> tobeConfirmedTradeQueue = new LinkedBlockingQueue<>();

    @PostConstruct
    private void start() {
        executorService.execute(new OrderChecker());
    }

    class OrderChecker implements Runnable {

        @Override
        public void run() {
            try {
                while (true) {
                    HuobiTrade trade = tobeConfirmedTradeQueue.take();
                    if(trade.getAction().equals(TradeAction.FAIL)) {
                        tradeEventMongoDao.insertTradeEvent(trade);
                        continue;
                    }
                    logger.info("Begin check trade {}", trade);
                    OrderDetail orderDetail = huobiTradeClient.queryOrder(trade.getOrderId());
                    logger.info("Huobi order detail {}", orderDetail);
                }
            } catch (Exception e) {
                logger.error("ERROR ", e);
            } catch (Throwable e) {
                logger.error("ERROR ", e);
            }


        }
    }

    public static BlockingQueue<HuobiTrade> getTobeConfirmedTradeQueue() {
        return tobeConfirmedTradeQueue;
    }

    //    @Override
//    public Receive createReceive() {
//
//        if(tradeEventMongoDao == null) {
//            tradeEventMongoDao = ApplicationContextHolder.getBean(TradeEventMongoDao.class);
//        }
//
//        return receiveBuilder().match(HuobiTrade.class, (trade) -> {
//            if(tradeEventMongoDao == null) {
//                logger.error("tradeEventMongoDao is null");
//                return;
//            }
//            tradeEventMongoDao.insertTradeEvent(trade);
//        }).build();
//    }
}
