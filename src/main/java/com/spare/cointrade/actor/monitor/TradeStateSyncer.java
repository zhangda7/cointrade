package com.spare.cointrade.actor.monitor;


import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import com.spare.cointrade.model.*;
import com.spare.cointrade.service.TradeHistoryService;
import com.spare.cointrade.trade.AccountManager;
import com.spare.cointrade.util.AkkaContext;
import com.spare.cointrade.util.CoinTradeConstants;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 同步、更新每次交易的状态
 * 提供接口，返回上一次的交易对是否已经交易成功
 * 创建订单的actor可以根据上一次是否成功的结果，判断是否要进行下一次交易
 */
public class TradeStateSyncer extends AbstractActor {

    private static Logger logger = LoggerFactory.getLogger(TradeStateSyncer.class);

    public static Props props () {
        return Props.create(TradeStateSyncer.class, () -> new TradeStateSyncer());
    }

    private static Map<String, TradePair> toCheckedPair = new HashMap<>();

    private static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1, new DefaultThreadFactory("TradeStateSyncer-"));

    private ActorSelection listingInfoMonitor;

    private static ReentrantLock reentrantLock = new ReentrantLock();

    public TradeStateSyncer() {
        listingInfoMonitor = AkkaContext.getSystem().actorSelection(
                AkkaContext.getFullActorName(CoinTradeConstants.ACTOR_LISTING_INFO_MONITOR));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(TradePair.class, (tradePair -> {
            try {
                reentrantLock.lock();
                logger.info("Receive trade pair {}", tradePair.getPairId());
                toCheckedPair.put(tradePair.getPairId(), tradePair);
                scheduledExecutorService.schedule(new MockUpdateTrade(), 5, TimeUnit.SECONDS);
                reentrantLock.unlock();
            } catch (Exception e) {
                logger.error("ERROR ", e);
            }
        })).build();
    }

    class MockUpdateTrade implements Runnable {

        @Override
        public void run() {
            reentrantLock.lock();
            for (Map.Entry<String, TradePair> entry : toCheckedPair.entrySet()) {
                try {
                    entry.getValue().getTradePair_1().setResult(TradeResult.SUCCESS);
                    entry.getValue().getTradePair_2().setResult(TradeResult.SUCCESS);
                    logger.info("Update pair {} to success", entry.getKey());
                    TradeHistoryService.INSTANCE.updatePairResult(entry.getKey(), TradeResult.SUCCESS.name());
                    listingInfoMonitor.tell(entry.getValue(), ActorRef.noSender());
                } catch (Exception e) {
                    logger.error("ERROR ", e);
                }
            }
            toCheckedPair.clear();
            reentrantLock.unlock();

        }

        private void balanceBinanceBTC(TradePair tradePair) {
            if(tradePair.getTradePair_1().getTradePlatform().equals(TradePlatform.BINANCE)) {
                SignalTrade signalTrade = tradePair.getTradePair_1();
//                signalTrade.ge
            }
            Double preAmount = AccountManager.INSTANCE.getFreeAmount(TradePlatform.BINANCE, CoinType.BTC);

        }
    }


}
