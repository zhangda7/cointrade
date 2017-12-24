package com.spare.cointrade.actor.trade;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import com.spare.cointrade.model.*;
import com.spare.cointrade.model.depth.HuobiDepth;
import com.spare.cointrade.realtime.okcoin.model.OkcoinDepth;
import com.spare.cointrade.policy.impl.Buy2Sell2PolicyImpl;
import com.spare.cointrade.realtime.huobi.HuobiClient;
import com.spare.cointrade.util.AkkaContext;
import com.spare.cointrade.util.ApplicationContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 对于收到的消息进行判决，判断是否要进行trade
 * Created by dada on 2017/8/20.
 */
public class TradeJudgeV2 extends AbstractActor {

    private static Logger logger = LoggerFactory.getLogger(TradeJudgeV2.class);

    public static Props props () {
        return Props.create(TradeJudgeV2.class, () -> new TradeJudgeV2());
    }

    private static final Double EXCHANGE_RATE = 6.67191524;

    //手续费
    public static final Double FIX_SERVICE_CHARGE = 0.002;

    private ActorSelection huobiTraderActor;

    private ActorSelection okCoinTraderActor;

    private Buy2Sell2PolicyImpl buy2Sell2Policy;

    public static CurStatus curStatus = new CurStatus();

    private AtomicInteger huobiNoDataCount = new AtomicInteger(0);

    private Long lastTradeTs = 0L;

    public TradeJudgeV2() {
        huobiBidsDepth = new TreeMap<>(new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                return o2.compareTo(o1);
            }
        });
        huobiAsksDepth = new TreeMap<>();
        okCoinAsksDepth = new TreeMap<>();
        okCoinBidsDepth = new TreeMap<>(new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                return o2.compareTo(o1);
            }
        });
        huobiTraderActor = AkkaContext.getSystem().actorSelection("akka://rootSystem/user/huobiTrader");
        okCoinTraderActor = AkkaContext.getSystem().actorSelection("akka://rootSystem/user/okCoinTrader");
        buy2Sell2Policy = new Buy2Sell2PolicyImpl();
    }

    /**
     * 火币网的最新卖N深度数据，BID=买入
     *  "bids": [
     [买1价,买1量]
     [买2价,买2量]
     */
//    private TreeMap<Double, Double> huobiBidsDepth;
    private TreeMap<Double, TradeDepth> huobiBidsDepth;


    /**
     * 火币网最新深度数据，ASK=卖出
     * "asks": [
     [卖1价,卖1量]
     [卖2价,卖2量]
     */
    private TreeMap<Double, TradeDepth> huobiAsksDepth;


    /**
     * okcoin最新的深度数据。永远最新
     */
    private TreeMap<Double, TradeDepth> okCoinBidsDepth;

    private TreeMap<Double, TradeDepth> okCoinAsksDepth;

    private long tsOfHuobi;

    private long tsOfOkCoin;

    private AtomicInteger tradeCount1 = new AtomicInteger(0);

    private AtomicInteger tradeCount2 = new AtomicInteger(0);

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(HuobiDepth.class, (depth -> {
            try {
                judgeClearCache();
                parseHuobi(depth);
            } catch (Exception e) {
                logger.error("ERROR ", e);
            }
            judge();
        })).match(OkcoinDepth.class, (depth) -> {
            try {
                judgeClearCache();
                parseOkCoin(depth);
            } catch (Exception e) {
                logger.error("ERROR ", e);
            }
            judge();
        }).build();
    }

    private void judgeClearCache() {
        long curTs = System.currentTimeMillis();

        if(curStatus.getHuobiDate() != null && curTs - curStatus.getHuobiDate().getTime() > 7000) {
            logger.warn("Begin clear huobi cache data");
            huobiBidsDepth.clear();
            huobiAsksDepth.clear();
            if(huobiNoDataCount.getAndIncrement() >= 1) {
                curStatus.setHuobiDate(null);
                huobiNoDataCount.set(0);
                logger.warn("Restart huobi client");
                ApplicationContextHolder.getBean(HuobiClient.class).startFetch();
            }
        } else if(curStatus.getOkCoinDate() != null && curTs - curStatus.getOkCoinDate().getTime() > 7000) {
            logger.warn("Begin clear okcoin cache data");
            okCoinBidsDepth.clear();
            okCoinAsksDepth.clear();
        }
    }

    public void judge() {

        List<TradeInfo> tradeInfoList = buy2Sell2Policy.canTrade(huobiBidsDepth, huobiAsksDepth,
                okCoinBidsDepth, okCoinAsksDepth,
                curStatus.getHuobiAccount(), curStatus.getOkCoinAccount());

        if(tradeInfoList == null) {
            return;
        }

        Double huobiCoinDelta = 0.0;
        Double huobiMoenyDelta = 0.0;

        Double okcoinCoinDelta = 0.0;
        Double okcoinMoneyDelta = 0.0;

        for (TradeInfo tradeInfo :tradeInfoList) {
            if(tradeInfo.getSource().equals(TradeSource.HUOBI)) {
                if(tradeInfo.getAction().equals(TradeAction.BUY)) {
                    huobiCoinDelta += tradeInfo.getAmount() * (1 - FIX_SERVICE_CHARGE);
                    huobiMoenyDelta -= tradeInfo.getAmount() * tradeInfo.getPrice() * (1 + FIX_SERVICE_CHARGE);
                } else if(tradeInfo.getAction().equals(TradeAction.SELL)) {
                    huobiCoinDelta -= tradeInfo.getAmount() * (1 + FIX_SERVICE_CHARGE);
                    huobiMoenyDelta += tradeInfo.getAmount() * tradeInfo.getPrice() * (1 - FIX_SERVICE_CHARGE);
                }
                huobiTraderActor.tell(tradeInfo, ActorRef.noSender());
            } else if(tradeInfo.getSource().equals(TradeSource.OKCOIN)) {
                if(tradeInfo.getAction().equals(TradeAction.BUY)) {
                    okcoinCoinDelta += tradeInfo.getAmount() * (1 - FIX_SERVICE_CHARGE);
                    okcoinMoneyDelta -= tradeInfo.getAmount() * tradeInfo.getPrice() * (1 + FIX_SERVICE_CHARGE);
                } else if(tradeInfo.getAction().equals(TradeAction.SELL)) {
                    okcoinCoinDelta -= tradeInfo.getAmount() * (1 + FIX_SERVICE_CHARGE);
                    okcoinMoneyDelta += tradeInfo.getAmount() * tradeInfo.getPrice() * (1 - FIX_SERVICE_CHARGE);
                }
                okCoinTraderActor.tell(tradeInfo, ActorRef.noSender());
            }
        }


        curStatus.getHuobiAccount().setCoinAmount(curStatus.getHuobiAccount().getCoinAmount() + huobiCoinDelta);
        curStatus.getHuobiAccount().setMoney(curStatus.getHuobiAccount().getMoney() + huobiMoenyDelta);

        curStatus.getOkCoinAccount().setCoinAmount(curStatus.getOkCoinAccount().getCoinAmount() + okcoinCoinDelta);
        curStatus.getOkCoinAccount().setMoney(curStatus.getOkCoinAccount().getMoney() + okcoinMoneyDelta);


        lastTradeTs = System.currentTimeMillis();

//        huobiTraderActor.tell(huobiTrade, ActorRef.noSender());
//        okCoinTraderActor.tell(okCoinTrade, ActorRef.noSender());

        //update data
//        huobiBuy1.setAmount(huobiBuy1.getAmount() - amount);
//        if(huobiBuy1.getAmount() == 0) {
//            huobiBidsDepth.remove(huobiBuy1.getPrice());
//        }
//        okCoinSell1.setAmount(okCoinSell1.getAmount() - amount);
//        if(okCoinSell1.getAmount() == 0) {
//            okCoinAsksDepth.remove(okCoinSell1.getPrice());
//        }

    }

    private void parseOkCoin(OkcoinDepth depth) {
        if(depth == null) {
            return;
        }
        if(depth.isClear()) {
            logger.info("Begin clear okcoin cache data");
            okCoinAsksDepth.clear();
            okCoinBidsDepth.clear();
            return;
        }
        tsOfOkCoin = depth.getTimestamp();
        curStatus.setOkCoinDate(new Date(tsOfOkCoin));
        if(depth.getAsks() != null) {
            updateOkCoin(depth.getAsks(), okCoinAsksDepth);
        }

        if(depth.getBids() != null) {
            updateOkCoin(depth.getBids(), okCoinBidsDepth);
        }
    }

    private void updateOkCoin(List<List<String>> datas, Map<Double, TradeDepth> okCoinDepth) {
        for (List<String> pair : datas) {
            Double price = Double.parseDouble(pair.get(0));
            Double count = Double.parseDouble(pair.get(1));
            if(count == 0) {
                okCoinDepth.remove(price);
                continue;
            }
            TradeDepth previous = okCoinDepth.get(price);
            if(previous == null) {
                previous = new TradeDepth();
                previous.setPrice(price);
                previous.setSource(TradeSource.OKCOIN);
            }
            previous.setAmount(count);
            okCoinDepth.put(price, previous);
        }
    }

    private void parseHuobi(HuobiDepth depth) {
        if(depth.getTick() == null) {
            return;
        }

        if(depth.isClear()) {
            logger.info("Begin clear huobi cache data");
            huobiAsksDepth.clear();
            huobiBidsDepth.clear();
            return;
        }

        //因为火币网每次都发送的是全量消息，所以每次先clear掉了
//        logger.info("Huobi Ask : {}", huobiAsksDepth.firstEntry());
//        logger.info("Huobi Bid : {}", huobiBidsDepth.lastEntry());
        tsOfHuobi = depth.getTick().getTs();
        curStatus.setHuobiDate(new Date(tsOfHuobi));
        huobiAsksDepth.clear();
        huobiBidsDepth.clear();
        if(depth.getTick().getAsks() != null) {
            for (List<Double> pair : depth.getTick().getAsks()) {
//                TradeDepth tradeDepth = new TradeDepth();
//                tradeDepth.setPrice(pair.get(0));
//                tradeDepth.setAmount(pair.get(1));
//                tradeDepth.setSource(TradeSource.HUOBI);
                huobiAsksDepth.put(pair.get(0), makeOneDepth(pair.get(0), pair.get(1), TradeSource.HUOBI));
            }
        }

        if(depth.getTick().getBids() != null) {
            for (List<Double> pair : depth.getTick().getBids()) {
//                TradeDepth tradeDepth = new TradeDepth();
//                tradeDepth.setPrice(pair.get(0));
//                tradeDepth.setAmount(pair.get(1));
//                tradeDepth.setSource(TradeSource.HUOBI);
                huobiBidsDepth.put(pair.get(0), makeOneDepth(pair.get(0), pair.get(1), TradeSource.HUOBI));
            }
        }
    }

    private TradeDepth makeOneDepth(Double price, Double amount, TradeSource source) {
        TradeDepth tradeDepth = new TradeDepth();
        tradeDepth.setPrice(price);
        tradeDepth.setAmount(amount);
        tradeDepth.setSource(TradeSource.HUOBI);
        return tradeDepth;
    }
}
