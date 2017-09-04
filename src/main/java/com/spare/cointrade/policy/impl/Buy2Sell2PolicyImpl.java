package com.spare.cointrade.policy.impl;

import com.spare.cointrade.model.*;
import com.spare.cointrade.util.CoinTradeConstants;
import com.spare.cointrade.util.CoinTradeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.print.DocumentPropertiesUI;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
                                    AccountInfo huobiAccount,
                                    AccountInfo okcoinAccount) {

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

//        TradeDepth huobiBuy2 = huobiBidsDepth.lowerEntry(huobiBidsDepth.firstKey()).getValue();
//
//        TradeDepth huobiSell2 = huobiAsksDepth.higherEntry(huobiAsksDepth.firstKey()).getValue();
//
//        TradeDepth okCoinBuy2 = okCoinBidsDepth.lowerEntry(okCoinBidsDepth.firstKey()).getValue();
//
//        TradeDepth okCoinSell2 = okCoinAsksDepth.higherEntry(okCoinAsksDepth.firstKey()).getValue();

        TradeDepth huobiBuy2 = indexEntry(huobiBidsDepth, 1).getValue();

        TradeDepth huobiSell2 = indexEntry(huobiAsksDepth, 1).getValue();

        TradeDepth okCoinBuy2 = indexEntry(okCoinBidsDepth, 1).getValue();

        TradeDepth okCoinSell2 = indexEntry(okCoinAsksDepth, 1).getValue();

        double maxBuy1Ratio = Math.max(huobiBuy2.getPrice(), okCoinSell2.getPrice()) * CoinTradeConstants.FIX_SERVICE_CHARGE * 2; // buy , sell, so * 2

        double maxBuy2Ratio = Math.max(huobiSell2.getPrice(), okCoinBuy2.getPrice()) * CoinTradeConstants.FIX_SERVICE_CHARGE * 2; // buy , sell, so * 2

        curStatus.setDelta1(huobiBuy2.getPrice() - okCoinSell2.getPrice());
        curStatus.setDelta2(okCoinBuy2.getPrice() - huobiSell2.getPrice());
        //这个赋值应该在parse的时候进行
        curStatus.setHuobiBuy1(huobiBuy2);
        curStatus.setHuobiSell1(huobiSell2);
        curStatus.setOkcoinBuy1(okCoinBuy2);
        curStatus.setOkcoinSell1(okCoinSell2);

        List<TradeInfo> tradeInfoList = null;

        if(huobiBuy2.getPrice() - okCoinSell2.getPrice() > maxBuy1Ratio) {
            tradeInfoList = tryDoTrade(okcoinAccount, huobiBuy2, huobiAccount, okCoinSell2);
            if(tradeInfoList == null) {
                return null;
            }
            if(huobiBuy2.getAmount() == 0) {
                huobiBidsDepth.remove(huobiBuy2.getPrice());
            }
            if(okCoinSell2.getAmount() == 0) {
                okCoinAsksDepth.remove(okCoinSell2.getPrice());
            }
        } else if(okCoinBuy2.getPrice() - huobiSell2.getPrice() > maxBuy2Ratio) {
            tradeInfoList = tryDoTrade(huobiAccount, okCoinBuy2, okcoinAccount, huobiSell2);
            if(tradeInfoList == null) {
                return null;
            }
            if(huobiSell2.getAmount() == 0) {
                huobiAsksDepth.remove(huobiSell2.getPrice());
            }
            if(okCoinBuy2.getAmount() == 0) {
                okCoinBidsDepth.remove(okCoinBuy2.getPrice());
            }
        }

        return tradeInfoList;

    }

    private Map.Entry<Double, TradeDepth> indexEntry(Map<Double, TradeDepth> depthMap, int index) {
        if(depthMap == null) {
            return null;
        }

        int i = 0;

        for (Map.Entry<Double, TradeDepth> entry : depthMap.entrySet()) {
            if(i++ >= index) {
                return entry;
            }
        }
        return null;
    }

    /**
     *
     * @param buyAccount 买方账户
     * @param buySource 市场上的bids info，买1买2...
     * @param sellAccount
     * @param sellSource  市场上的ask info，卖1卖2...
     * @return
     */
    private List<TradeInfo> tryDoTrade(AccountInfo buyAccount, TradeDepth buySource, AccountInfo sellAccount, TradeDepth sellSource) {

        long curTs = System.currentTimeMillis();

        Double amount = Math.min(buySource.getAmount(), sellSource.getAmount());

        amount = Math.min(amount, CoinTradeContext.MAX_TRADE_AMOUNT);

        amount = Math.min(amount, sellAccount.getCoinAmount());

        logger.info("Try trade {} [{} {}] [{} {}]", sellAccount.getCoinAmount(),
                buySource.getPrice(), buySource.getAmount(),
                sellSource.getPrice(), sellSource.getAmount());

        if(amount < 0.0099999) {
            logger.info("Min amount is {} < 0.1, return {} {}", amount, sellAccount.getCoinAmount(), Math.min(buySource.getAmount(), sellSource.getAmount()));
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

        if(buyAccount.getMoney() < sellSource.getPrice() * amount) {
            logger.info("buy source {} money is {}, less than {}, return", buyAccount.getSource(), buyAccount.getMoney(),
                    sellSource.getPrice() * sellSource.getAmount());
            return null;
        }

        List<TradeInfo> tradeInfos = new ArrayList<>();

        TradeInfo sellTrade = new TradeInfo();
        sellTrade.setAmount(amount);
        sellTrade.setPrice(buySource.getPrice());
        sellTrade.setAction(TradeAction.SELL);
        sellTrade.setTs(curTs);
        sellTrade.setSource(buySource.getSource());

        logger.info("Can trade for {}", sellTrade);

        //buy okcoin
        TradeInfo buyTrade = new TradeInfo();
        buyTrade.setAmount(amount);
        buyTrade.setPrice(sellSource.getPrice());
        buyTrade.setAction(TradeAction.BUY);
        buyTrade.setSource(sellSource.getSource());
        buyTrade.setTs(curTs);

        logger.info("Can trade for {}", buyTrade);

        buySource.setAmount(buySource.getAmount() - amount);
        sellSource.setAmount(sellSource.getAmount() - amount);

        tradeInfos.add(sellTrade);
        tradeInfos.add(buyTrade);

        return tradeInfos;

    }

}
