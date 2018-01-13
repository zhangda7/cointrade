package com.spare.cointrade.actor.trade;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import com.alibaba.fastjson.JSON;
import com.spare.cointrade.ExchangeContext;
import com.spare.cointrade.actor.monitor.ListingInfoMonitor;
import com.spare.cointrade.log.TradeChanceLog;
import com.spare.cointrade.model.*;
import com.spare.cointrade.service.TradeHistoryService;
import com.spare.cointrade.trade.AccountManager;
import com.spare.cointrade.util.AkkaContext;
import com.spare.cointrade.util.CoinTradeConstants;
import com.spare.cointrade.util.ListingDepthUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class TradeJudgeV3 {

    private static Logger logger = LoggerFactory.getLogger(TradeJudgeV3.class);

    private static Logger tradeChanceLogger = LoggerFactory.getLogger(TradeChanceLog.class);

    //    public static Props props () {
//        return Props.create(TradeJudgeV3.class, () -> new TradeJudgeV3());
//    }
    private ActorSelection tradeStateSyncer;

    private static final Double MIN_TRADE_AMOUNT = 0.01;

    public static Map<String, OrderBookEntry> chanceTradeMap = new ConcurrentHashMap<>();

    public static Ewma normalizeProfit = new Ewma();

    private static AtomicLong pairIdGenerator = new AtomicLong();

    private static boolean canTrade = true;

    private Map<CoinType, Double> minCoinTradeAmountMap = new HashMap<>();

    public static void setCanTrade(boolean canTrade) {
        TradeJudgeV3.canTrade = canTrade;
    }

    private static final Double REVERSE_AVERAGE_NORMALIZE_PERCENT = 0.98;

    private static final Double REVERSE_TOTAL_NORLAIZE_PERCENT = 0.3;

    /**
     * 总是监控深度信息的第二级，即买2，卖2
     */
    private static final int MONITOR_DEPTH_LEVEL = 1;

    public TradeJudgeV3() {
        this.tradeStateSyncer = AkkaContext.getSystem().actorSelection(
                AkkaContext.getFullActorName(CoinTradeConstants.ACTOR_TRADE_STATE_SYNCER));
        minCoinTradeAmountMap.put(CoinType.BTC, 0.0001);
        minCoinTradeAmountMap.put(CoinType.LTC, 0.01);
        minCoinTradeAmountMap.put(CoinType.ETH, 0.01);
    }

    //    @Override
//    public Receive createReceive() {
//        return null;
//    }

    private static final Double DECREASE_PERCENT = 0.8;

    private void doTrade(TradePair tradePair) {
//        for (SignalTrade signalTrade : tradePair.getSignalTradeList()) {
//            mockDoTrade(signalTrade);
//        }
        if(! canTrade) {
//            logger.debug("Current has unconfirmed trade, can not trade now");
            return;
        }
        tradePair.setPairId(String.valueOf(pairIdGenerator.incrementAndGet()));
        double total_1 = tradePair.getTradePair_1().getAmount() * tradePair.getTradePair_1().getNormalizePrice();
        double total_2 = tradePair.getTradePair_2().getAmount() * tradePair.getTradePair_2().getNormalizePrice();
        double profit = 0.0;
        if(tradePair.getTradePair_1().getTradeAction().equals(TradeAction.SELL)) {
            profit = total_1 - total_2;
        } else {
            profit = total_2 - total_1;
        }
        ExchangeContext.addProfit(profit);
        mockDoTrade(tradePair, tradePair.getTradePair_1(), profit);
        mockDoTrade(tradePair, tradePair.getTradePair_2(), profit);
        canTrade = false;
        this.tradeStateSyncer.tell(tradePair, ActorRef.noSender());
    }

    private void mockDoTrade(TradePair tradePair, SignalTrade signalTrade, double profit) {
        TradeHistory tradeHistory = new TradeHistory();
        tradeHistory.setPairId(tradePair.getPairId());
        tradeHistory.setDirection(tradePair.getTradeDirection().name());
        tradeHistory.setTradePlatform(signalTrade.getTradePlatform());
        tradeHistory.setTradeAction(signalTrade.getTradeAction());
        tradeHistory.setCoinType(signalTrade.getSourceCoin());
        tradeHistory.setTargetCoinType(signalTrade.getTargetCoin());
        tradeHistory.setPrice(signalTrade.getPrice());
        tradeHistory.setAmount(signalTrade.getAmount());
        tradeHistory.setResult(TradeResult.TRADING);
        tradeHistory.setProfit(profit);
        Account account = AccountManager.INSTANCE.getPlatformAccountMap().get(signalTrade.getTradePlatform());
        Balance sourceBalance = account.getBalanceMap().get(signalTrade.getSourceCoin());
        Balance targetBalance = null;
        if(signalTrade.getTargetCoin().equals(CoinType.MONRY)) {
            targetBalance = account.getMoneyBalance();
        }
//        Balance targetBalance = account.getBalanceMap().get(signalTrade.getTargetCoin());
        tradeHistory.setPreAccountSourceAmount(sourceBalance.getFreeAmount());
        tradeHistory.setPreAccountTargetAmount(targetBalance.getFreeAmount());
        if(signalTrade.getTradeAction().equals(TradeAction.BUY)) {
            sourceBalance.setFreeAmount(sourceBalance.getFreeAmount() + signalTrade.getAmount());
            targetBalance.setFreeAmount(targetBalance.getFreeAmount() - signalTrade.getAmount() * signalTrade.getPrice());
        } else if(signalTrade.getTradeAction().equals(TradeAction.SELL)) {
            sourceBalance.setFreeAmount(sourceBalance.getFreeAmount() - signalTrade.getAmount());
            targetBalance.setFreeAmount(targetBalance.getFreeAmount() + signalTrade.getAmount() * signalTrade.getPrice());
        } else {
            throw new IllegalArgumentException("Illegal trade action " + signalTrade.getTradeAction());
        }
        tradeHistory.setAfterAccountSourceAmount(sourceBalance.getFreeAmount());
        tradeHistory.setAfterAccountTargetAmount(targetBalance.getFreeAmount());
        tradeHistory.setAccountName(account.getAccountName());
//        logger.info("Prepare insert tradeHistory {}", JSON.toJSONString(tradeHistory));
        TradeHistoryService.INSTANCE.insert(tradeHistory);

    }

    public void findTradeChance() {
        updateTradeChanceMap();

        List<TradePair> tradePairList = findNormalTradeChance();
        if(tradePairList == null) {
            logger.warn("Not found trade pair for normal direction");
//            return;
        }
        for(TradePair tradePair : tradePairList) {
            tradeChanceLogger.info("Find tradePair {}", JSON.toJSONString(tradePair));
            doTrade(tradePair);
        }

        List<TradePair> reverseTradePairList = findReverseTradeChance();
        if(reverseTradePairList == null) {
            logger.warn("Not found reverse trade pair for normal direction");
            return;
        }
        for(TradePair tradePair : reverseTradePairList) {
            tradeChanceLogger.info("Find reverse tradePair {}", JSON.toJSONString(tradePair));
            doTrade(tradePair);
        }

    }

    public String toiListingInfoKey(TradePlatform tradePlatform, CoinType coinType) {
        StringBuilder sb = new StringBuilder(200);
        sb.append(tradePlatform.name() + "_");
        sb.append(TradeType.COIN_COIN.name() + "_");
        sb.append(coinType.name() + "_");
        return sb.toString();
    }

    /**
     * 寻找正向的交易机会
     * 即找最小差价和最大差价，进行盈利的交易
     * @return
     */
    private List<TradePair> findNormalTradeChance() {
//        double minTotalAmount = Math.min(maxEntry.getAmount() * maxEntry.getNormaliseDelta(), minEntry.getNormaliseDelta() * minEntry.getAmount());
        List<TradePair> tradePairList = new ArrayList<>();
        List<OrderBookEntry> entryList = new ArrayList<>();
        entryList.addAll(chanceTradeMap.values());
        Collections.sort(entryList, new Comparator<OrderBookEntry>() {
            @Override
            public int compare(OrderBookEntry o1, OrderBookEntry o2) {
                return (int) (o2.getNormaliseDelta() - o1.getNormaliseDelta());
            }
        });
        for(OrderBookEntry orderBookEntry : entryList) {
//            logger.info("Find max and min entry {} {}", JSON.toJSONString(orderBookEntry), JSON.toJSONString(orderBookEntry));
            TradePair maxDeltaPair = createTradePair(orderBookEntry);
            if(maxDeltaPair == null) {
                continue;
            }
            //注意，这里使用绝对值了
            normalizeProfit.setValue(Math.abs(orderBookEntry.getNormaliseDelta()));
            tradePairList.add(maxDeltaPair);
        }
        return tradePairList;
    }

    /**
     * 寻找正向的交易机会
     * 即找最小差价和最大差价，进行盈利的交易
     * @return
     */
    private List<TradePair> findReverseTradeChance() {
        List<TradePair> tradePairList = new ArrayList<>();
        List<OrderBookEntry> entryList = new ArrayList<>();
        entryList.addAll(chanceTradeMap.values());
        Collections.sort(entryList, new Comparator<OrderBookEntry>() {
            @Override
            public int compare(OrderBookEntry o1, OrderBookEntry o2) {
                return (int) (o1.getNormaliseDelta() - o2.getNormaliseDelta());
            }
        });
        for(OrderBookEntry orderBookEntry : entryList) {
            if(Math.abs(orderBookEntry.getNormaliseDelta()) > normalizeProfit.getValue() * REVERSE_AVERAGE_NORMALIZE_PERCENT) {
                continue;
            }

            tradeChanceLogger.info("Can do reverse trade, cur delta {}, EWMA delta{}",
                    orderBookEntry.getNormaliseDelta(), normalizeProfit.getValue(), JSON.toJSONString(orderBookEntry));
            TradePair maxDeltaPair = createReverseTradePair(orderBookEntry);
            if(maxDeltaPair == null) {
                continue;
            }
            normalizeProfit.setValue(Math.abs(orderBookEntry.getNormaliseDelta()) * -1);
            tradePairList.add(maxDeltaPair);
        }
        return tradePairList;
    }

    /**
     * 进行反转的交易判断
     * 2种情况:
     * 1.delta > 0， platform 1 > platform 2
     *  platform 1 sell, platform 2 buy
     *
     * 2.delta < 0
     *  platform 1 buy, platform 2 buy
     * @param maxEntry
     * @return
     */
    private TradePair createReverseTradePair(OrderBookEntry maxEntry) {
        ListingFullInfo fullInfo1 = ListingInfoMonitor.listingFullInfoMap.get(toiListingInfoKey(maxEntry.getPlatform1(), maxEntry.getCoinType()));
        ListingFullInfo fullInfo2 = ListingInfoMonitor.listingFullInfoMap.get(toiListingInfoKey(maxEntry.getPlatform2(), maxEntry.getCoinType()));
        if(maxEntry.getDelta() > 0) {
            return judgeAndMakePair(fullInfo1, fullInfo2, maxEntry, TradeDirection.REVERSE);
        } else {
            return judgeAndMakePair(fullInfo2, fullInfo1, maxEntry, TradeDirection.REVERSE);
        }
    }

    /**
     * 对于差价最大的2个平台进行判断
     * 2种情况:
     * 1.delta > 0， platform 1 > platform 2
     *  platform 1 sell, platform 2 buy
     *
     * 2.delta < 0
     *  platform 1 buy, platform 2 buy
     * @param maxEntry
     * @return
     */
    private TradePair createTradePair(OrderBookEntry maxEntry) {
        ListingFullInfo fullInfo1 = ListingInfoMonitor.listingFullInfoMap.get(toiListingInfoKey(maxEntry.getPlatform1(), maxEntry.getCoinType()));
        ListingFullInfo fullInfo2 = ListingInfoMonitor.listingFullInfoMap.get(toiListingInfoKey(maxEntry.getPlatform2(), maxEntry.getCoinType()));
        if(maxEntry.getDelta() > 0) {
            return judgeAndMakePair(fullInfo2, fullInfo1, maxEntry, TradeDirection.FORWARD);
        } else {
            return judgeAndMakePair(fullInfo1, fullInfo2, maxEntry, TradeDirection.FORWARD);
        }

    }

    private TradePair judgeAndMakePair(ListingFullInfo buySide, ListingFullInfo sellSide,
                                       OrderBookEntry orderBookEntry, TradeDirection tradeDirection) {
        TradePair tradePair = new TradePair();
        tradePair.setTradeDirection(tradeDirection);
//        double maxSellAmount = Math.min(sellSide.getBuyDepth().getDepthInfoMap().firstEntry().getValue().getAmount(),
//                AccountManager.INSTANCE.getFreeAmount(sellSide.getTradePlatform(), orderBookEntry.getCoinType()));
        double maxSellAmount = Math.min(ListingDepthUtil.getLevelDepthInfo(sellSide.getBuyDepth(), MONITOR_DEPTH_LEVEL).getAmount(),
                AccountManager.INSTANCE.getFreeAmount(sellSide.getTradePlatform(), orderBookEntry.getCoinType()));
//        double maxBuyAmount = AccountManager.INSTANCE.getNormalizeCNY(buySide.getTradePlatform()) /
//                buySide.getSellDepth().getDepthInfoMap().firstEntry().getValue().getNormalizePrice();
        double maxBuyAmount = AccountManager.INSTANCE.getNormalizeCNY(buySide.getTradePlatform()) /
                ListingDepthUtil.getLevelDepthInfo(buySide.getSellDepth(), MONITOR_DEPTH_LEVEL).getNormalizePrice();

        double minAmount = Math.min(maxSellAmount, maxBuyAmount);

        Double coinMinTradeAmount = minCoinTradeAmountMap.get(buySide.getSourceCoinType());
        if(coinMinTradeAmount == null) {
            coinMinTradeAmount = MIN_TRADE_AMOUNT;
        }

        if(tradeDirection.equals(TradeDirection.REVERSE)) {
            /**
             * 把总收益的百分比也加上，反转时不能超出这个阈值
             */
            double maxReverseMoney = ExchangeContext.totalProfit * REVERSE_TOTAL_NORLAIZE_PERCENT;

            //暂时使用买方的归一化价格来计算最小交易数量
//        double minAmount2 = maxReverseMoney / buySide.getBuyDepth().getDepthInfoMap().firstEntry().getValue().getNormalizePrice();
            double minAmount2 = maxReverseMoney / ListingDepthUtil.getLevelDepthInfo(buySide.getBuyDepth(), MONITOR_DEPTH_LEVEL).getNormalizePrice();

            minAmount = Math.min(minAmount, minAmount2);
        }


        if(minAmount < coinMinTradeAmount) {
            return null;
        }

//        tradePair.setTradePair_1(makeOneTrade(sellSide.getTradePlatform(),
//                orderBookEntry.getCoinType(), CoinType.MONRY, // 应该为buySide.targetCoinType
//                TradeAction.SELL,
//                sellSide.getBuyDepth().getDepthInfoMap().firstEntry().getValue().getOriPrice(), minAmount,
//                sellSide.getBuyDepth().getDepthInfoMap().firstEntry().getValue().getNormalizePrice()));
        ListingDepth.DepthInfo sellInfo = ListingDepthUtil.getLevelDepthInfo(sellSide.getBuyDepth(), MONITOR_DEPTH_LEVEL);
        tradePair.setTradePair_1(makeOneTrade(sellSide.getTradePlatform(),
                orderBookEntry.getCoinType(), CoinType.MONRY, // 应该为buySide.targetCoinType
                TradeAction.SELL,
                sellInfo.getOriPrice(), minAmount,
                sellInfo.getNormalizePrice()));
//        tradePair.setTradePair_2(makeOneTrade(buySide.getTradePlatform(),
//                orderBookEntry.getCoinType(), CoinType.MONRY,
//                TradeAction.BUY,
//                buySide.getBuyDepth().getDepthInfoMap().firstEntry().getValue().getOriPrice(), minAmount,
//                buySide.getBuyDepth().getDepthInfoMap().firstEntry().getValue().getNormalizePrice()));
        //TODO FIXME 这里错了吧，应该是sell depth
        ListingDepth.DepthInfo buyInfo = ListingDepthUtil.getLevelDepthInfo(buySide.getBuyDepth(), MONITOR_DEPTH_LEVEL);
        tradePair.setTradePair_2(makeOneTrade(buySide.getTradePlatform(),
                orderBookEntry.getCoinType(), CoinType.MONRY,
                TradeAction.BUY,
                buyInfo.getOriPrice(), minAmount,
                buyInfo.getNormalizePrice()));
        return tradePair;
    }

    /**
     * 对于已经计算好的2个交易对，找到最小的归一化价格，再调整他们的交易数量
     * @param maxDeltaPair
     * @param minDeltaPair
     */
    private void adjustMinAmount(TradePair maxDeltaPair, TradePair minDeltaPair) {

    }

    public SignalTrade makeOneTrade(TradePlatform tradePlatform, CoinType sourceCoin, CoinType targetCoin,
                                    TradeAction tradeAction, Double price, Double amount, Double normalizePrice) {
        SignalTrade signalTrade = new SignalTrade();
        signalTrade.setTradePlatform(tradePlatform);
        signalTrade.setSourceCoin(sourceCoin);
        signalTrade.setTargetCoin(targetCoin);
        signalTrade.setTradeAction(tradeAction);
        signalTrade.setPrice(price);
        signalTrade.setNormalizePrice(normalizePrice);
        signalTrade.setAmount(amount);
        return signalTrade;
    }

    private boolean hasRemaingBalance(TradePlatform tradePlatform, CoinType coinType, Double amount) {
        if(! AccountManager.INSTANCE.getPlatformAccountMap().get(tradePlatform).getBalanceMap().containsKey(coinType)) {
            logger.error("{} do not has coin type {}", tradePlatform, coinType);
            return false;
        }
        if(AccountManager.INSTANCE.getPlatformAccountMap().get(tradePlatform).
                getBalanceMap().get(coinType).getFreeAmount() >= amount) {
            return true;
        }
        logger.warn("{} {} amount is {} < {}, just skip this chance", tradePlatform, coinType, AccountManager.INSTANCE.getPlatformAccountMap().get(tradePlatform).
                getBalanceMap().get(coinType).getFreeAmount(), amount);
        return false;
    }

    /**
     * 检查交易的机会
     */
    private void updateTradeChanceMap() {

        for (CoinType coinType : ExchangeContext.toCheckedCoin) {
            Map<TradePlatform, ListingFullInfo> fullInfoMap = new HashMap<>();
            for (Map.Entry<String, ListingFullInfo> entry : ListingInfoMonitor.listingFullInfoMap.entrySet()) {
                if(! entry.getValue().getSourceCoinType().equals(coinType)) {
                    continue;
                }
                fullInfoMap.put(entry.getValue().getTradePlatform(), entry.getValue());
            }
            Map<String, OrderBookEntry> orderBookEntryMap = checkOneCoinTradeChance(coinType, fullInfoMap);
            if(orderBookEntryMap == null) {
                continue;
            }
            chanceTradeMap.putAll(orderBookEntryMap);
        }
    }

    private Map<String, OrderBookEntry> checkOneCoinTradeChance(CoinType coinType, Map<TradePlatform, ListingFullInfo> fullInfoMap) {
        if(fullInfoMap.size() < 2) {
            logger.warn("Coin {} list is {} < 2, just skip", coinType, fullInfoMap.size());
            return null;
        }
        Map<String, OrderBookEntry> orderBookEntryMap = new HashMap<>();
        Iterator<TradePlatform> tradePlatformIterable = fullInfoMap.keySet().iterator();
        TradePlatform pre = null;
        //使用笛卡尔积的方式，对全部的platform进行组合计算
        while (tradePlatformIterable.hasNext()) {
            if(pre == null) {
                pre = tradePlatformIterable.next();
            }
            TradePlatform cur = tradePlatformIterable.next();
            if(cur == null) {
                break;
            }
            ListingFullInfo fullInfo1 = fullInfoMap.get(pre);
            ListingFullInfo fullInfo2 = fullInfoMap.get(cur);
            OrderBookEntry orderBookEntry = checkTradeChanceBy2Platform(coinType, fullInfo1, fullInfo2);
            orderBookEntryMap.put(orderBookEntry.toKey(), orderBookEntry);
            pre = cur;
        }
        return orderBookEntryMap;

    }

    /**
     * 仅计算2个平台关于某一个币的价格差
     * @param coinType
     * @param fullInfo1
     * @param fullInfo2
     * @return
     */
    private OrderBookEntry checkTradeChanceBy2Platform(CoinType coinType, ListingFullInfo fullInfo1, ListingFullInfo fullInfo2) {
        ListingDepth.DepthInfo fullSellInfo_1 = ListingDepthUtil.getLevelDepthInfo(fullInfo1.getSellDepth(), MONITOR_DEPTH_LEVEL);
        ListingDepth.DepthInfo fullSellInfo_2 = ListingDepthUtil.getLevelDepthInfo(fullInfo2.getSellDepth(), MONITOR_DEPTH_LEVEL);

        double delta = fullSellInfo_1.getNormalizePrice() - fullSellInfo_2.getNormalizePrice();
        double amount = Math.min(fullSellInfo_1.getAmount(), fullSellInfo_2.getAmount());
//        double delta = fullInfo1.getSellDepth().getDepthInfoMap().firstEntry().getValue().getNormalizePrice() -
//                fullInfo2.getSellDepth().getDepthInfoMap().firstEntry().getValue().getNormalizePrice();
//
//        double amount = Math.min(fullInfo1.getSellDepth().getDepthInfoMap().firstEntry().getValue().getAmount(),
//                fullInfo2.getSellDepth().getDepthInfoMap().firstEntry().getValue().getAmount());

        OrderBookEntry orderBookEntry = new OrderBookEntry();
        orderBookEntry.setCoinType(coinType);
        orderBookEntry.setAmount(amount);
        orderBookEntry.setDelta(Math.abs(delta));
        if(delta >= 0) {
            orderBookEntry.setPlatform1(fullInfo1.getTradePlatform());
            orderBookEntry.setPlatform2(fullInfo2.getTradePlatform());
//            orderBookEntry.setNormaliseDelta(10000 /
//                    fullInfo2.getSellDepth().getDepthInfoMap().firstEntry().getValue().getNormalizePrice() *
//                    fullInfo1.getSellDepth().getDepthInfoMap().firstEntry().getValue().getNormalizePrice() - 10000);
            orderBookEntry.setNormaliseDelta(10000 /
                    fullSellInfo_2.getNormalizePrice() *
                    fullSellInfo_1.getNormalizePrice() - 10000);
        } else {
            orderBookEntry.setPlatform1(fullInfo2.getTradePlatform());
            orderBookEntry.setPlatform2(fullInfo1.getTradePlatform());
//            orderBookEntry.setNormaliseDelta(10000 /
//                    fullInfo1.getSellDepth().getDepthInfoMap().firstEntry().getValue().getNormalizePrice() *
//                    fullInfo2.getSellDepth().getDepthInfoMap().firstEntry().getValue().getNormalizePrice() - 10000);
            orderBookEntry.setNormaliseDelta(10000 /
                    fullSellInfo_1.getNormalizePrice() *
                    fullSellInfo_2.getNormalizePrice() - 10000);
        }

        return orderBookEntry;
    }

}
