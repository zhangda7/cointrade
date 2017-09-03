package com.spare.cointrade.policy.impl;

import com.spare.cointrade.model.*;
import com.spare.cointrade.util.CoinTradeConstants;
import com.spare.cointrade.util.CoinTradeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 查看深度信息的policy
 * 统一使用买2和卖2 的信息交易
 * 触发条件：任意一个买2 > 卖2, 则可以触发了
 * Created by dada on 2017/9/3.
 */
public class Buy2Sell2PolicyImpl {

    private static Logger logger = LoggerFactory.getLogger(Buy2Sell2PolicyImpl.class);

    public static CurStatus curStatus = new CurStatus();

    private AtomicInteger tradeCount = new AtomicInteger(0);

    private Long lastTradeTs = 0L;

    /**
     *
     * @param huobiBidsDepth source1 买入深度
     * @param huobiAsksDepth source1 卖出深度
     * @param okCoinBidsDepth
     * @param okCoinAsksDepth
     * @return
     */
    public List<TradeInfo> canTrade(TreeMap<Double, TradeDepth> huobiBidsDepth,
                            TreeMap<Double, TradeDepth> huobiAsksDepth,
                            TreeMap<Double, TradeDepth> okCoinBidsDepth,
                            TreeMap<Double, TradeDepth> okCoinAsksDepth,
                                    Double huobiCoinAmount,
                                    Double okcoinCoinAmount) {

        if(huobiBidsDepth.size() == 0 || okCoinAsksDepth.size() == 0 || huobiAsksDepth.size() == 0 || okCoinBidsDepth.size() == 0) {
            return null;
        }

//        TradeDepth huobiBuy1 = huobiBidsDepth.firstEntry().getValue();
//
//        TradeDepth huobiSell1 = huobiAsksDepth.firstEntry().getValue();
//
//        TradeDepth okCoinBuy1 = okCoinBidsDepth.firstEntry().getValue();
//
//        TradeDepth okCoinSell1 = okCoinAsksDepth.firstEntry().getValue();

        TradeDepth huobiBuy2 = huobiBidsDepth.lowerEntry(huobiBidsDepth.firstKey()).getValue();

        TradeDepth huobiSell2 = huobiAsksDepth.higherEntry(huobiAsksDepth.firstKey()).getValue();

        TradeDepth okCoinBuy2 = okCoinBidsDepth.lowerEntry(okCoinBidsDepth.firstKey()).getValue();

        TradeDepth okCoinSell2 = okCoinAsksDepth.higherEntry(okCoinAsksDepth.firstKey()).getValue();

        double maxBuy1Ratio = Math.max(huobiBuy2.getPrice(), okCoinSell2.getPrice()) * CoinTradeConstants.FIX_SERVICE_CHARGE * 2; // buy , sell, so * 2

        double maxBuy2Ratio = Math.max(huobiSell2.getPrice(), okCoinBuy2.getPrice()) * CoinTradeConstants.FIX_SERVICE_CHARGE * 2; // buy , sell, so * 2

        curStatus.setDelta1(huobiBuy2.getPrice() - okCoinSell2.getPrice());
        curStatus.setDelta2(okCoinBuy2.getPrice() - huobiSell2.getPrice());
        //这个赋值应该在parse的时候进行
        curStatus.setHuobiBuy1(huobiBuy2);
        curStatus.setHuobiSell1(huobiSell2);
        curStatus.setOkcoinBuy1(okCoinBuy2);
        curStatus.setOkcoinSell1(okCoinSell2);

        if(huobiBuy2.getPrice() - okCoinSell2.getPrice() > maxBuy1Ratio) {
            return tryDoTrade(huobiCoinAmount, huobiBuy2, okCoinSell2);
        } else if(okCoinBuy2.getPrice() - huobiSell2.getPrice() > maxBuy2Ratio) {
            return tryDoTrade(okcoinCoinAmount, okCoinBuy2, huobiSell2);
        }

        return null;

    }

    private List<TradeInfo> tryDoTrade(Double accountCoinAmount, TradeDepth buySource, TradeDepth sellSource) {

        long curTs = System.currentTimeMillis();

        Double amount = Math.min(buySource.getAmount(), sellSource.getAmount());

        amount = Math.min(amount, CoinTradeContext.MAX_TRADE_AMOUNT);

        amount = Math.min(amount, accountCoinAmount);

        logger.info("Try trade {} [{} {}] [{} {}]", accountCoinAmount,
                buySource.getPrice(), buySource.getAmount(),
                sellSource.getPrice(), sellSource.getAmount());

        if(amount < 0.0099999) {
            logger.info("Min amount is {} < 0.1, return {} {}", amount, accountCoinAmount, Math.min(buySource.getAmount(), sellSource.getAmount()));
            return null;
        }

        if(CoinTradeContext.MAX_TRADE_COUNT > 0 && tradeCount.getAndIncrement() >= CoinTradeContext.MAX_TRADE_COUNT) {
            logger.info("Trade count reach max count {}, return", tradeCount.get());
            return null;
        }

        if(System.currentTimeMillis() - lastTradeTs < 1000) {
            logger.info("trade too fast, return");
            return null;
        }

        //TODO
//        if(curStatus.getOkCoinAccount().getMoney() < okCoinTrade.getPrice() * amount) {
//            logger.info("OK coin money is {}, less than {}, return", curStatus.getOkCoinAccount().getMoney(),
//                    okCoinTrade.getPrice() * okCoinTrade.getAmount());
//            return null;
//        }

        List<TradeInfo> tradeInfos = new ArrayList<>();

        TradeInfo sellTrade = new TradeInfo();
        sellTrade.setAmount(amount);
        sellTrade.setPrice(buySource.getPrice());
        sellTrade.setAction(TradeAction.SELL);
        sellTrade.setTs(curTs);
        sellTrade.setSource(buySource.getSource());



//            huobiTraderActor.tell(huobiTrade, ActorRef.noSender());
        //buy okcoin
        TradeInfo buyTrade = new TradeInfo();
        buyTrade.setAmount(amount);
        buyTrade.setPrice(sellSource.getPrice());
        buyTrade.setAction(TradeAction.BUY);
        buyTrade.setSource(sellSource.getSource());
        buyTrade.setTs(curTs);

        tradeInfos.add(sellTrade);
        tradeInfos.add(buyTrade);

        return tradeInfos;

    }

}
