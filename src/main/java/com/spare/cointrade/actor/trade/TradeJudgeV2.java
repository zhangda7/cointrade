package com.spare.cointrade.actor.trade;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import com.spare.cointrade.model.*;
import com.spare.cointrade.model.depth.HuobiDepth;
import com.spare.cointrade.model.depth.OkcoinDepth;
import com.spare.cointrade.model.trade.HuobiTrade;
import com.spare.cointrade.model.trade.OkCoinTrade;
import com.spare.cointrade.policy.impl.Buy2Sell2PolicyImpl;
import com.spare.cointrade.util.AkkaContext;
import com.spare.cointrade.util.CoinTradeContext;
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
    private static final Double FIX_SERVICE_CHARGE = 0.002;

    private ActorSelection huobiTraderActor;

    private ActorSelection okCoinTraderActor;

    private Buy2Sell2PolicyImpl buy2Sell2Policy;

    public static CurStatus curStatus = new CurStatus();

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
                parseHuobi(depth);
            } catch (Exception e) {
                logger.error("ERROR ", e);
            }
            judge();
        })).match(OkcoinDepth.class, (depth) -> {
            try {
                parseOkCoin(depth);
            } catch (Exception e) {
                logger.error("ERROR ", e);
            }
            judge();
        }).build();
    }

    public void judge() {

        List<TradeInfo> tradeInfoList = buy2Sell2Policy.canTrade(huobiBidsDepth, huobiAsksDepth,
                okCoinBidsDepth, okCoinAsksDepth,
                curStatus.getHuobiAccount().getCoinAmount(), curStatus.getOkCoinAccount().getCoinAmount());

        if(tradeInfoList == null) {
            return;
        }

        for (TradeInfo tradeInfo :tradeInfoList) {
            if(tradeInfo.getSource().equals(TradeSource.HUOBI)) {
                if(tradeInfo.getAction().equals(TradeAction.BUY)) {

                } else if(tradeInfo.getAction().equals(TradeAction.SELL)) {

                }
            } else if(tradeInfo.getSource().equals(TradeSource.OKCOIN)) {
                if(tradeInfo.getAction().equals(TradeAction.BUY)) {

                } else if(tradeInfo.getAction().equals(TradeAction.SELL)) {

                }
            }
        }

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
