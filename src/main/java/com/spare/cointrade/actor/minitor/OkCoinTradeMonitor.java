package com.spare.cointrade.actor.minitor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.spare.cointrade.dao.TradeEventMongoDao;
import com.spare.cointrade.model.TradeAction;
import com.spare.cointrade.model.trade.OkCoinTrade;
import com.spare.cointrade.trade.okcoin.OkCoinTradeClient;
import com.spare.cointrade.trade.okcoin.OrderDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.concurrent.*;

/**
 * 监控已经提交的订单状态，并取消失败的订单
 * Created by dada on 2017/8/25.
 */
public class OkCoinTradeMonitor {

    private static Logger logger = LoggerFactory.getLogger(OkCoinTradeMonitor.class);

    @Autowired
    private TradeEventMongoDao tradeEventMongoDao;

    @Autowired
    private OkCoinTradeClient okCoinTradeClient;

    private static ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
            .setNameFormat("okcoin-monitor-").build();

    private static ExecutorService executorService = Executors.newSingleThreadExecutor(namedThreadFactory);

    private static BlockingQueue<OkCoinTrade> tobeConfirmedTradeQueue = new LinkedBlockingQueue<>();

    @PostConstruct
    private void start() {
        executorService.execute(new OrderChecker());
    }

    class OrderChecker implements Runnable {

        @Override
        public void run() {
            try {
                while (true) {
                    OkCoinTrade trade = tobeConfirmedTradeQueue.take();
                    if(trade.getAction().equals(TradeAction.FAIL)) {
                        tradeEventMongoDao.insertTradeEvent(trade);
                        continue;
                    }
                    logger.info("Begin check trade {}", trade);
                    OrderDetail orderDetail = okCoinTradeClient.queryOrder(trade.getOrderId());
                    logger.info("Huobi order detail {}", orderDetail);
                }
            } catch (Exception e) {
                logger.error("ERROR ", e);
            } catch (Throwable e) {
                logger.error("ERROR ", e);
            }


        }
    }

    public static BlockingQueue<OkCoinTrade> getTobeConfirmedTradeQueue() {
        return tobeConfirmedTradeQueue;
    }

}
