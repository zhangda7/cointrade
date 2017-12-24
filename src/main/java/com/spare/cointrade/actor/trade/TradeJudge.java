package com.spare.cointrade.actor.trade;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import com.spare.cointrade.model.CurStatus;
import com.spare.cointrade.model.TradeAction;
import com.spare.cointrade.model.TradeDepth;
import com.spare.cointrade.model.depth.HuobiDepth;
import com.spare.cointrade.realtime.okcoin.model.OkcoinDepth;
import com.spare.cointrade.model.trade.HuobiTrade;
import com.spare.cointrade.model.trade.OkCoinTrade;
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
public class TradeJudge extends AbstractActor {

    private static Logger logger = LoggerFactory.getLogger(TradeJudge.class);

    public static Props props () {
        return Props.create(TradeJudge.class, () -> new TradeJudge());
    }

    private static final Double EXCHANGE_RATE = 6.67191524;

    //手续费
    private static final Double FIX_SERVICE_CHARGE = 0.002;

    private ActorSelection huobiTraderActor;

    private ActorSelection okCoinTraderActor;

    public static CurStatus curStatus = new CurStatus();

    private Long lastTradeTs = 0L;

    public TradeJudge() {
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
        } else if(curStatus.getOkCoinDate() != null && curTs - curStatus.getOkCoinDate().getTime() > 7000) {
            logger.warn("Begin clear okcoin cache data");
            okCoinBidsDepth.clear();
            okCoinAsksDepth.clear();
        }
    }

    public void judge() {
        long curTs = System.currentTimeMillis();
//        logger.info("TS Delta {}, {}", curTs - tsOfHuobi, curTs - tsOfOkCoin);
        if(huobiBidsDepth.size() == 0 || okCoinAsksDepth.size() == 0 || huobiAsksDepth.size() == 0 || okCoinBidsDepth.size() == 0) {
            return;
        }
//        logger.info("Detal-1 {}", huobiAsksDepth.firstEntry().getKey() - okCoinBidsDepth.firstEntry().getKey());
//
//        logger.info("Detal-2 {}", huobiBidsDepth.firstEntry().getKey() - okCoinAsksDepth.firstEntry().getKey());

        TradeDepth huobiBuy1 = huobiBidsDepth.firstEntry().getValue();

        TradeDepth huobiSell1 = huobiAsksDepth.firstEntry().getValue();

        TradeDepth okCoinBuy1 = okCoinBidsDepth.firstEntry().getValue();

        TradeDepth okCoinSell1 = okCoinAsksDepth.firstEntry().getValue();

        double maxBuy1Ratio = Math.max(huobiBuy1.getPrice(), okCoinSell1.getPrice()) * FIX_SERVICE_CHARGE * 2; // buy , sell, so * 2

        double maxBuy2Ratio = Math.max(huobiSell1.getPrice(), okCoinBuy1.getPrice()) * FIX_SERVICE_CHARGE * 2; // buy , sell, so * 2

//        logger.info("[1] Buy delta {}, service charge {}", huobiBuy1.getPrice() - okCoinSell1.getPrice(), maxBuy1Ratio);
//        logger.info("[2] Buy delta {}, service charge {}", okCoinBuy1.getPrice() - huobiSell1.getPrice(), maxBuy2Ratio);


        curStatus.setDelta1(huobiBuy1.getPrice() - okCoinSell1.getPrice());
        curStatus.setDelta2(okCoinBuy1.getPrice() - huobiSell1.getPrice());

        //这个赋值应该在parse的时候进行
        curStatus.setHuobiBuy1(huobiBuy1);
        curStatus.setHuobiSell1(huobiSell1);
        curStatus.setOkcoinBuy1(okCoinBuy1);
        curStatus.setOkcoinSell1(okCoinSell1);

//        Double minAmount = Math.min(curStatus.getHuobiAccount().getCoinAmount(), curStatus.getOkCoinAccount().getCoinAmount());

        //TODO 应该保存一下上次的状态，如果第一个价格发生了变化，才会再次去交易的
        if(huobiBuy1.getPrice() - okCoinSell1.getPrice() > maxBuy1Ratio) {
            // sell huobi
            //TODO 还需增加是否超出自身余额的判断
            //TODO 相同的触发后，不能重复交易的。自己把对应的数目减掉就好了

            Double minAmount = curStatus.getHuobiAccount().getCoinAmount();

            Double amount = Math.min(huobiBuy1.getAmount(), okCoinSell1.getAmount());

            amount = Math.min(amount, CoinTradeContext.MAX_TRADE_AMOUNT);

            amount = Math.min(amount, minAmount);

            if(amount < 0.0099999) {
                logger.info("Min amount is {} < 0.1, return {} {}", amount, minAmount, Math.min(huobiBuy1.getAmount(), okCoinSell1.getAmount()));
                return;
            }

            if(CoinTradeContext.MAX_TRADE_COUNT > 0 && tradeCount1.getAndIncrement() >= CoinTradeContext.MAX_TRADE_COUNT) {
                logger.info("count 1 reach max count {}, return", tradeCount1.get());
                return;
            }

            if(System.currentTimeMillis() - lastTradeTs < 1000) {
                logger.info("trade too fast, return");
                return;
            }

            HuobiTrade huobiTrade = new HuobiTrade();
            huobiTrade.setAmount(amount);
            huobiTrade.setPrice(huobiBuy1.getPrice());
            huobiTrade.setAction(TradeAction.SELL);
            huobiTrade.setTs(curTs);
//            huobiTraderActor.tell(huobiTrade, ActorRef.noSender());
            //buy okcoin
            OkCoinTrade okCoinTrade = new OkCoinTrade();
            okCoinTrade.setAmount(amount);
            okCoinTrade.setPrice(okCoinSell1.getPrice());
            okCoinTrade.setAction(TradeAction.BUY);

            if(curStatus.getOkCoinAccount().getMoney() < okCoinTrade.getPrice() * amount) {
                logger.info("OK coin money is {}, less than {}, return", curStatus.getOkCoinAccount().getMoney(),
                        okCoinTrade.getPrice() * okCoinTrade.getAmount());
                return;
            }
            curStatus.getHuobiAccount().setCoinAmount(curStatus.getHuobiAccount().getCoinAmount() - amount);
            curStatus.getHuobiAccount().setMoney(curStatus.getHuobiAccount().getMoney() + huobiBuy1.getPrice() * amount);

            curStatus.getOkCoinAccount().setCoinAmount(curStatus.getOkCoinAccount().getCoinAmount() + amount);
            curStatus.getOkCoinAccount().setMoney(curStatus.getOkCoinAccount().getMoney() - okCoinSell1.getPrice() * amount);


            lastTradeTs = System.currentTimeMillis();

            huobiTraderActor.tell(huobiTrade, ActorRef.noSender());
            okCoinTraderActor.tell(okCoinTrade, ActorRef.noSender());

            //update data
            huobiBuy1.setAmount(huobiBuy1.getAmount() - amount);
            if(huobiBuy1.getAmount() == 0) {
                huobiBidsDepth.remove(huobiBuy1.getPrice());
            }
            okCoinSell1.setAmount(okCoinSell1.getAmount() - amount);
            if(okCoinSell1.getAmount() == 0) {
                okCoinAsksDepth.remove(okCoinSell1.getPrice());
            }
        } else if(okCoinBuy1.getPrice() - huobiSell1.getPrice() > maxBuy2Ratio) {
            // sell huobi
            //TODO 还需增加是否超出自身余额的判断

            Double minAmount = curStatus.getOkCoinAccount().getCoinAmount();

            Double amount = Math.min(okCoinBuy1.getAmount(), huobiSell1.getAmount());

            amount = Math.min(amount, CoinTradeContext.MAX_TRADE_AMOUNT);

            amount = Math.min(amount, minAmount);

            if(amount < 0.0099999) {
                logger.info("Min amount is {} < 0.1, return {} {}", amount, minAmount, Math.min(okCoinBuy1.getAmount(), huobiSell1.getAmount()));
                return;
            }

            if( CoinTradeContext.MAX_TRADE_COUNT > 0 && tradeCount2.getAndIncrement() >= CoinTradeContext.MAX_TRADE_COUNT) {
                logger.info("count 2 reach max count {}, return", tradeCount2.get());
                return;
            }

            if(System.currentTimeMillis() - lastTradeTs < 1000) {
                logger.info("trade too fast, return");
                return;
            }

            HuobiTrade huobiTrade = new HuobiTrade();
            huobiTrade.setAmount(amount);
            huobiTrade.setPrice(huobiSell1.getPrice());
            huobiTrade.setAction(TradeAction.BUY);
            huobiTrade.setTs(curTs);
//            huobiTraderActor.tell(huobiTrade, ActorRef.noSender());
            //buy okcoin
            OkCoinTrade okCoinTrade = new OkCoinTrade();
            okCoinTrade.setAmount(amount);
            okCoinTrade.setPrice(okCoinBuy1.getPrice());
            okCoinTrade.setAction(TradeAction.SELL);

            if(curStatus.getHuobiAccount().getMoney() < huobiSell1.getPrice() * amount) {
                logger.info("Huobi money is {}, less than {}, return", curStatus.getHuobiAccount().getMoney(),
                        huobiTrade.getPrice() * huobiTrade.getAmount());
                return;
            }
            curStatus.getHuobiAccount().setCoinAmount(curStatus.getHuobiAccount().getCoinAmount() + amount);
            curStatus.getHuobiAccount().setMoney(curStatus.getHuobiAccount().getMoney() - huobiSell1.getPrice() * amount);

            curStatus.getOkCoinAccount().setCoinAmount(curStatus.getOkCoinAccount().getCoinAmount() - amount);
            curStatus.getOkCoinAccount().setMoney(curStatus.getOkCoinAccount().getMoney() + okCoinBuy1.getPrice() * amount);

            lastTradeTs = System.currentTimeMillis();

            huobiTraderActor.tell(huobiTrade, ActorRef.noSender());
            okCoinTraderActor.tell(okCoinTrade, ActorRef.noSender());

            //update data
            okCoinBuy1.setAmount(okCoinBuy1.getAmount() - amount);
            if(okCoinBuy1.getAmount() == 0) {
                okCoinBidsDepth.remove(okCoinBuy1.getPrice());
            }
            huobiSell1.setAmount(huobiSell1.getAmount() - amount);
            if(huobiSell1.getAmount() == 0) {
                huobiAsksDepth.remove(huobiSell1.getPrice());
            }
        }

//        logger.info("Delta-3 {}", huobiBidsDepth.firstEntry().getKey() - okCoinBidsDepth.firstEntry().getKey());
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
                TradeDepth tradeDepth = new TradeDepth();
                tradeDepth.setPrice(pair.get(0));
                tradeDepth.setAmount(pair.get(1));
                huobiAsksDepth.put(pair.get(0), tradeDepth);
            }
        }

        if(depth.getTick().getBids() != null) {
            for (List<Double> pair : depth.getTick().getBids()) {
                TradeDepth tradeDepth = new TradeDepth();
                tradeDepth.setPrice(pair.get(0));
                tradeDepth.setAmount(pair.get(1));
                huobiBidsDepth.put(pair.get(0), tradeDepth);
            }
        }
    }
}
