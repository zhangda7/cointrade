package com.spare.cointrade.actor.trade;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import com.spare.cointrade.model.TradeAction;
import com.spare.cointrade.model.depth.HuobiDepth;
import com.spare.cointrade.model.depth.OkcoinDepth;
import com.spare.cointrade.model.trade.HuobiTrade;
import com.spare.cointrade.model.trade.OkCoinTrade;
import com.spare.cointrade.util.AkkaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.zip.DeflaterOutputStream;

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

    public TradeJudge() {
        huobiBidsDepth = new TreeMap<>(new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                return (int) (o2 - o1);
            }
        });
        huobiAsksDepth = new TreeMap<>();
        okCoinAsksDepth = new TreeMap<>();
        okCoinBidsDepth = new TreeMap<>(new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                return (int) (o2 - o1);
            }
        });
        huobiTraderActor = AkkaContext.getSystem().actorSelection("akka://rootSystem/user/huobiTrader");
        okCoinTraderActor = AkkaContext.getSystem().actorSelection("akka://rootSystem/user/okCoinTrader");
    }

    /**
     * 火币网的最新卖N深度数据，BID=买入
     */
    private TreeMap<Double, Double> huobiBidsDepth;

    /**
     * 火币网最新深度数据，ASK=卖出
     */
    private TreeMap<Double, Double> huobiAsksDepth;


    /**
     * okcoin最新的深度数据。永远最新
     */
    private TreeMap<Double, Double> okCoinBidsDepth;

    private TreeMap<Double, Double> okCoinAsksDepth;

    private long tsOfHuobi;

    private long tsOfOkCoin;

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
        long curTs = System.currentTimeMillis();
        logger.info("TS Delta {}, {}", curTs - tsOfHuobi, curTs - tsOfOkCoin);
        if(huobiBidsDepth.size() == 0 || okCoinAsksDepth.size() == 0 || huobiAsksDepth.size() == 0 || okCoinBidsDepth.size() == 0) {
            return;
        }
//        logger.info("Detal-1 {}", huobiAsksDepth.firstEntry().getKey() - okCoinBidsDepth.firstEntry().getKey());
//
//        logger.info("Detal-2 {}", huobiBidsDepth.firstEntry().getKey() - okCoinAsksDepth.firstEntry().getKey());

        double huobiBuy1 = huobiBidsDepth.firstEntry().getKey();

        double okCoinBuy1 = okCoinBidsDepth.firstEntry().getKey();

        double maxBuy1Ratio = Math.max(huobiBuy1, okCoinBuy1) * FIX_SERVICE_CHARGE;

        logger.info("Buy delta {}, service charge {}", Math.abs(huobiBuy1 - okCoinBuy1), maxBuy1Ratio);

        if(huobiBuy1 - okCoinBuy1 > maxBuy1Ratio) {
            // sell huobi
            HuobiTrade huobiTrade = new HuobiTrade();
            huobiTrade.setAmount(0.01);
            huobiTrade.setPrice(huobiBuy1);
            huobiTrade.setAction(TradeAction.SELL);
            huobiTraderActor.tell(huobiTrade, ActorRef.noSender());
            //buy okcoin
            OkCoinTrade okCoinTrade = new OkCoinTrade();
            okCoinTrade.setAmount(0.01);
            okCoinTrade.setPrice(okCoinBuy1);
            okCoinTrade.setAction(TradeAction.BUY);
            okCoinTraderActor.tell(okCoinTrade, ActorRef.noSender());
        } else if(okCoinBuy1 - huobiBuy1 > maxBuy1Ratio) {
            // sell huobi
            HuobiTrade huobiTrade = new HuobiTrade();
            huobiTrade.setAmount(0.01);
            huobiTrade.setPrice(huobiBuy1);
            huobiTrade.setAction(TradeAction.BUY);
            huobiTraderActor.tell(huobiTrade, ActorRef.noSender());
            //buy okcoin
            OkCoinTrade okCoinTrade = new OkCoinTrade();
            okCoinTrade.setAmount(0.01);
            okCoinTrade.setPrice(okCoinBuy1);
            okCoinTrade.setAction(TradeAction.SELL);
            okCoinTraderActor.tell(okCoinTrade, ActorRef.noSender());
        }

//        logger.info("Delta-3 {}", huobiBidsDepth.firstEntry().getKey() - okCoinBidsDepth.firstEntry().getKey());
    }

    private void parseOkCoin(OkcoinDepth depth) {
        if(depth == null) {
            return;
        }
        tsOfOkCoin = depth.getTimestamp();

        if(depth.getAsks() != null) {
            for (List<String> pair : depth.getAsks()) {
                Double price = Double.parseDouble(pair.get(0));
                Double count = Double.parseDouble(pair.get(1));
                if(count == 0) {
                    okCoinAsksDepth.remove(price);
                    continue;
                }
                okCoinAsksDepth.put(price, count);
            }
        }

        if(depth.getBids() != null) {
            for (List<String> pair : depth.getBids()) {
                Double price = Double.parseDouble(pair.get(0));
                Double count = Double.parseDouble(pair.get(1));
                if(count == 0) {
                    okCoinBidsDepth.remove(price);
                    continue;
                }
                okCoinBidsDepth.put(price, count);
            }
        }

    }

    private void parseHuobi(HuobiDepth depth) {
        if(depth.getTick() == null) {
            return;
        }
        //因为火币网每次都发送的是全量消息，所以每次先clear掉了
//        logger.info("Huobi Ask : {}", huobiAsksDepth.firstEntry());
//        logger.info("Huobi Bid : {}", huobiBidsDepth.lastEntry());
        tsOfHuobi = depth.getTick().getTs();
        huobiAsksDepth.clear();
        huobiBidsDepth.clear();
        if(depth.getTick().getAsks() != null) {
            for (List<Double> pair : depth.getTick().getAsks()) {
                huobiAsksDepth.put(pair.get(0), pair.get(1));
            }
        }

        if(depth.getTick().getBids() != null) {
            for (List<Double> pair : depth.getTick().getBids()) {
                huobiBidsDepth.put(pair.get(0), pair.get(1));
            }
        }
    }
}
