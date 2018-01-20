package com.spare.cointrade.actor.monitor;


import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import com.spare.cointrade.model.*;
import com.spare.cointrade.service.TradeHistoryService;
import com.spare.cointrade.util.AkkaContext;
import com.spare.cointrade.util.CoinTradeConstants;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
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

    private static final Object HOLDER = new Object();

    private static ConcurrentHashMap<String, TradePair> toCheckedPair = new ConcurrentHashMap<>();

    private static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1, new DefaultThreadFactory("TradeStateSyncer-"));

    private ActorSelection listingInfoMonitor;

    private static ReentrantLock reentrantLock = new ReentrantLock();

    public TradeStateSyncer() {
        listingInfoMonitor = AkkaContext.getSystem().actorSelection(
                AkkaContext.getFullActorName(CoinTradeConstants.ACTOR_LISTING_INFO_MONITOR));

        scheduledExecutorService.scheduleWithFixedDelay(new MockUpdateTrade(), 5, 5, TimeUnit.SECONDS);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(TradePair.class, (tradePair -> {
            try {
//                reentrantLock.lock();
                logger.info("Receive trade pair {}", tradePair.getPairId());
                toCheckedPair.put(tradePair.getPairId(), tradePair);
//                reentrantLock.unlock();
            } catch (Exception e) {
                logger.error("ERROR ", e);
            }
        })).build();
    }

    class MockUpdateTrade implements Runnable {

        @Override
        public void run() {
            if(! reentrantLock.tryLock()) {
                return;
            }
            reentrantLock.lock();
            Set<String> toRemove = new HashSet<>();
            for (TradePair tradePair : toCheckedPair.values()) {
                try {
                    syncSignalTradeResult(tradePair.getTradePair_1());
                    syncSignalTradeResult(tradePair.getTradePair_2());
                    syncSignalTradeResult(tradePair.getTradePair_3());
                    if(tradePair.getTradePair_1().getResult().equals(TradeResult.SUCCESS) &&
                            tradePair.getTradePair_2().getResult().equals(TradeResult.SUCCESS)) {
                        if(tradePair.getTradePair_3() != null) {
                            if(tradePair.getTradePair_3().getResult().equals(TradeResult.SUCCESS)) {
                                toRemove.add(tradePair.getPairId());
                            }
                        } else {
                            toRemove.add(tradePair.getPairId());
                        }
                    }
                    logger.info("Update pair {} to success", tradePair.getPairId());
                    TradeHistoryService.INSTANCE.updatePairResult(tradePair.getPairId(), TradeResult.SUCCESS.name());
                    listingInfoMonitor.tell(tradePair, ActorRef.noSender());
                } catch (Exception e) {
                    logger.error("ERROR ", e);
                }
            }
            for (String key : toRemove) {
                toCheckedPair.remove(key);
            }
            toRemove.clear();
            reentrantLock.unlock();
        }

        private void syncSignalTradeResult(SignalTrade signalTrade) {
            if(signalTrade == null) {
                return;
            }
            if(signalTrade.getResult() != null && signalTrade.getResult().equals(TradeResult.SUCCESS)) {
                return;
            }
            signalTrade.setResult(TradeResult.SUCCESS);
        }


    }


}
