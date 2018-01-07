package com.spare.cointrade.actor.trade;

import akka.actor.AbstractActor;
import akka.actor.Props;
import com.alibaba.fastjson.JSON;
import com.spare.cointrade.actor.monitor.ListingInfoMonitor;
import com.spare.cointrade.model.*;
import com.spare.cointrade.service.TradeHistoryService;
import com.spare.cointrade.trade.AccountManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TradeJudgeV3 {

    private static Logger logger = LoggerFactory.getLogger(TradeJudgeV3.class);

//    public static Props props () {
//        return Props.create(TradeJudgeV3.class, () -> new TradeJudgeV3());
//    }

    private static final Double MIN_TRADE_AMOUNT = 0.001;

    public static Map<String, OrderBookEntry> chanceTradeMap = new ConcurrentHashMap<>();

//    @Override
//    public Receive createReceive() {
//        return null;
//    }

    private static final Double DECREASE_PERCENT = 0.8;

    private void doTrade(TradePair tradePair) {
        for (SignalTrade signalTrade : tradePair.getSignalTradeList()) {
            mockDoTrade(signalTrade);
        }
    }

    private void mockDoTrade(SignalTrade signalTrade) {
        TradeHistory tradeHistory = new TradeHistory();
        tradeHistory.setTradePlatform(signalTrade.getTradePlatform());
        tradeHistory.setTradeAction(signalTrade.getTradeAction());
        tradeHistory.setCoinType(signalTrade.getSourceCoin());
        tradeHistory.setTargetCoinType(signalTrade.getTargetCoin());
        tradeHistory.setPrice(signalTrade.getPrice());
        tradeHistory.setAmount(signalTrade.getAmount());
        tradeHistory.setResult(TradeResult.TRADING);
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
            targetBalance.setFreeAmount(targetBalance.getFreeAmount() - signalTrade.getAmount());
        } else if(signalTrade.getTradeAction().equals(TradeAction.SELL)) {
            sourceBalance.setFreeAmount(sourceBalance.getFreeAmount() - signalTrade.getAmount());
            targetBalance.setFreeAmount(targetBalance.getFreeAmount() + signalTrade.getAmount());
        } else {
            throw new IllegalArgumentException("Illegal trade action " + signalTrade.getTradeAction());
        }
        tradeHistory.setAfterAccountSourceAmount(sourceBalance.getFreeAmount());
        tradeHistory.setAfterAccountTargetAmount(targetBalance.getFreeAmount());
        tradeHistory.setAccountName(account.getAccountName());
        logger.info("Prepare insert tradeHistory {}", JSON.toJSONString(tradeHistory));
        TradeHistoryService.INSTANCE.insert(tradeHistory);
    }

    public void findTradeChance() {
        updateTradeChanceMap();
        TradePair tradePair = findNormalTradeChance();
        if(tradePair == null) {
            logger.warn("Not found trade pair for normal direction");
            return;
        }
        logger.info("Find tradePair {}", JSON.toJSONString(tradePair));
        doTrade(tradePair);
    }

//    public String toBithumbKey(CoinType coinType) {
//        StringBuilder sb = new StringBuilder(200);
//        sb.append(TradePlatform.BITHUMB.name() + "_");
//        sb.append(TradeType.COIN_COIN.name() + "_");
//        sb.append(coinType.name() + "_");
//        sb.append(CoinType.KRW.name() + "_");
//        return sb.toString();
//    }
//
//    public String toBinanceKey(CoinType coinType) {
//        StringBuilder sb = new StringBuilder(200);
//        sb.append(TradePlatform.BINANCE.name() + "_");
//        sb.append(TradeType.COIN_COIN.name() + "_");
//        sb.append(coinType.name() + "_");
//        sb.append(CoinType.USDT.name() + "_");
//        return sb.toString();
//    }


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
    private TradePair findNormalTradeChance() {
        OrderBookEntry minEntry = null;
        OrderBookEntry maxEntry = null;
        for(OrderBookEntry orderBookEntry : chanceTradeMap.values()) {
            if(minEntry == null) {
                minEntry = orderBookEntry;
            }
            if(maxEntry == null) {
                maxEntry = orderBookEntry;
            }
            //实际是delta 差值
            //使用绝对值进行计算
            if(Math.abs(minEntry.getNormaliseDelta()) > Math.abs(orderBookEntry.getNormaliseDelta())) {
                minEntry = orderBookEntry;
            }
            if(Math.abs(maxEntry.getNormaliseDelta()) < Math.abs(orderBookEntry.getNormaliseDelta())) {
                maxEntry = orderBookEntry;
            }
        }
        if(minEntry.getCoinType().equals(maxEntry.getCoinType())) {
            return null;
        }

//        /**
//         * 最小的一个归一化交易总值
//         */
//        double minTotalAmount = Math.min(maxEntry.getAmount() * maxEntry.getNormaliseDelta(), minEntry.getNormaliseDelta() * minEntry.getAmount());

        logger.info("Find max and min entry {} {}", JSON.toJSONString(maxEntry), JSON.toJSONString(minEntry));

        TradePair maxDeltaPair = createTradePair(maxEntry);
//        TradePair minDeltaPair = createTradePair(minEntry, false, minTotalAmount);
        return maxDeltaPair;
//        ListingFullInfo bithumnSell1 = ListingInfoMonitor.listingFullInfoMap.get(toiListingInfoKey(maxEntry.getmaxEntry.getCoinType()));
//        ListingFullInfo binanceBuy1 = ListingInfoMonitor.listingFullInfoMap.get(toiListingInfoKey(maxEntry.getCoinType()));
//        ListingFullInfo bithumnBuy1 = ListingInfoMonitor.listingFullInfoMap.get(toiListingInfoKey(minEntry.getCoinType()));
//        ListingFullInfo binanceSell1 = ListingInfoMonitor.listingFullInfoMap.get(toiListingInfoKey(minEntry.getCoinType()));

//        double bithumbSellTotal = bithumnSell1.getBuyDepth().getDepthInfoMap().firstEntry().getValue().getTotalNormalizePrice();
//        double binanceBuy1Total = binanceBuy1.getSellDepth().getDepthInfoMap().firstEntry().getValue().getTotalNormalizePrice();
//
//        double bithumbBuy1Total = bithumnBuy1.getSellDepth().getDepthInfoMap().firstEntry().getValue().getTotalNormalizePrice();
//        double binanceSell1Total = binanceSell1.getBuyDepth().getDepthInfoMap().firstEntry().getValue().getTotalNormalizePrice();
//        double minTotal = Math.min(Math.min(binanceBuy1Total, binanceSell1Total), Math.min(bithumbBuy1Total, bithumbSellTotal));
//
//        double bithumbSell1Amount = minTotal / bithumnSell1.getBuyDepth().getDepthInfoMap().firstEntry().getValue().getNormalizePrice();
//        double binanceBuy1Amount = minTotal / binanceBuy1.getSellDepth().getDepthInfoMap().firstEntry().getValue().getNormalizePrice();
//
//        double maxDeltaMinAmount = Math.min(bithumbSell1Amount, binanceBuy1Amount);
//        maxDeltaMinAmount = Math.min(maxDeltaMinAmount, Math.min(
//                AccountManager.INSTANCE.getPlatformAccountMap().get(TradePlatform.BITHUMB).
//                        getBalanceMap().get(maxEntry.getCoinType()).getFreeAmount(),
//                AccountManager.INSTANCE.getPlatformAccountMap().get(TradePlatform.BINANCE).
//                        getBalanceMap().get(maxEntry.getCoinType()).getFreeAmount()));
//        if(! hasRemaingBalance(TradePlatform.BITHUMB, maxEntry.getCoinType(), maxDeltaMinAmount)) {
//            return null;
//        }
//        if(! hasRemaingBalance(TradePlatform.BINANCE, maxEntry.getCoinType(), maxDeltaMinAmount)) {
//            return null;
//        }
//        double bithumbBuy1Amount = minTotal / bithumnBuy1.getSellDepth().getDepthInfoMap().firstEntry().getValue().getNormalizePrice();
//        double binanceSell1Amount = binanceSell1.getBuyDepth().getDepthInfoMap().firstEntry().getValue().getNormalizePrice();
//
//        double minDeltaMinAmount = Math.min(bithumbBuy1Amount, binanceSell1Amount);
//        minDeltaMinAmount = Math.min(minDeltaMinAmount, Math.min(
//                AccountManager.INSTANCE.getPlatformAccountMap().get(TradePlatform.BITHUMB).
//                        getBalanceMap().get(minEntry.getCoinType()).getFreeAmount(),
//                AccountManager.INSTANCE.getPlatformAccountMap().get(TradePlatform.BINANCE).
//                        getBalanceMap().get(minEntry.getCoinType()).getFreeAmount()));
//        if(! hasRemaingBalance(TradePlatform.BITHUMB, minEntry.getCoinType(), minDeltaMinAmount)) {
//            return null;
//        }
//        if(! hasRemaingBalance(TradePlatform.BINANCE, minEntry.getCoinType(), minDeltaMinAmount)) {
//            return null;
//        }

//        TradePair tradePair = new TradePair();
////        //TODO 调整这里的price赋值
////        tradePair.getSignalTradeList().add(makeOneTrade(TradePlatform.BITHUMB, maxEntry.getCoinType(), CoinType.KRW, TradeAction.SELL, 1.0, bithumbSell1Amount));
////        tradePair.getSignalTradeList().add(makeOneTrade(TradePlatform.BINANCE, maxEntry.getCoinType(), CoinType.CNY, TradeAction.BUY, 1.0, binanceBuy1Amount));
////        tradePair.getSignalTradeList().add(makeOneTrade(TradePlatform.BITHUMB, minEntry.getCoinType(), CoinType.KRW, TradeAction.BUY, 1.0, bithumbBuy1Amount));
////        tradePair.getSignalTradeList().add(makeOneTrade(TradePlatform.BINANCE, minEntry.getCoinType(), CoinType.CNY, TradeAction.SELL, 1.0, binanceSell1Amount));
//
//        return tradePair;
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
        double minAmount = 0.0;
        TradePair tradePair = new TradePair();
        if(maxEntry.getDelta() > 0) {
            double maxSellAmount = Math.min(fullInfo1.getBuyDepth().getDepthInfoMap().firstEntry().getValue().getAmount(),
                    AccountManager.INSTANCE.getFreeAmount(fullInfo1.getTradePlatform(), maxEntry.getCoinType()));

            double maxBuyAmount = AccountManager.INSTANCE.getNormalizeCNY(fullInfo2.getTradePlatform()) /
                    fullInfo2.getSellDepth().getDepthInfoMap().firstEntry().getValue().getNormalizePrice();

            minAmount = Math.min(maxSellAmount, maxBuyAmount);

            if(minAmount < MIN_TRADE_AMOUNT) {
                return null;
            }
            tradePair.getSignalTradeList().add(makeOneTrade(fullInfo1.getTradePlatform(),
                    maxEntry.getCoinType(), CoinType.MONRY,
                    TradeAction.SELL,
                    fullInfo1.getBuyDepth().getDepthInfoMap().firstEntry().getValue().getOriPrice(), minAmount,
                    fullInfo1.getBuyDepth().getDepthInfoMap().firstEntry().getValue().getNormalizePrice()));
            tradePair.getSignalTradeList().add(makeOneTrade(fullInfo2.getTradePlatform(),
                    maxEntry.getCoinType(), CoinType.MONRY,
                    TradeAction.BUY,
                    fullInfo2.getBuyDepth().getDepthInfoMap().firstEntry().getValue().getOriPrice(), minAmount,
                    fullInfo2.getBuyDepth().getDepthInfoMap().firstEntry().getValue().getNormalizePrice()));
            return tradePair;
        } else {
            double maxSellAmount = Math.min(fullInfo2.getBuyDepth().getDepthInfoMap().firstEntry().getValue().getAmount(),
                    AccountManager.INSTANCE.getFreeAmount(fullInfo2.getTradePlatform(), maxEntry.getCoinType()));

            double maxBuyAmount = AccountManager.INSTANCE.getNormalizeCNY(fullInfo1.getTradePlatform()) /
                    fullInfo1.getSellDepth().getDepthInfoMap().firstEntry().getValue().getNormalizePrice();

            minAmount = Math.min(maxSellAmount, maxBuyAmount);

            if(minAmount < MIN_TRADE_AMOUNT) {
                return null;
            }

            tradePair.getSignalTradeList().add(makeOneTrade(fullInfo2.getTradePlatform(),
                    maxEntry.getCoinType(), CoinType.MONRY,
                    TradeAction.SELL,
                    fullInfo2.getBuyDepth().getDepthInfoMap().firstEntry().getValue().getOriPrice(), minAmount,
                    fullInfo2.getBuyDepth().getDepthInfoMap().firstEntry().getValue().getNormalizePrice()));

            tradePair.getSignalTradeList().add(makeOneTrade(fullInfo1.getTradePlatform(),
                    maxEntry.getCoinType(), CoinType.MONRY,
                    TradeAction.BUY,
                    fullInfo1.getBuyDepth().getDepthInfoMap().firstEntry().getValue().getOriPrice(), minAmount,
                    fullInfo1.getBuyDepth().getDepthInfoMap().firstEntry().getValue().getNormalizePrice()));
            return tradePair;
        }

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
        CoinType[] toCheckedCoin = new CoinType[] {CoinType.BTC, CoinType.ETH, CoinType.LTC};
        for (CoinType coinType : toCheckedCoin) {
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
        double delta = fullInfo1.getSellDepth().getDepthInfoMap().firstEntry().getValue().getNormalizePrice() -
                fullInfo2.getSellDepth().getDepthInfoMap().firstEntry().getValue().getNormalizePrice();

        double amount = Math.min(fullInfo1.getSellDepth().getDepthInfoMap().firstEntry().getValue().getAmount(),
                fullInfo2.getSellDepth().getDepthInfoMap().firstEntry().getValue().getAmount());

        OrderBookEntry orderBookEntry = new OrderBookEntry();
        orderBookEntry.setCoinType(coinType);
        orderBookEntry.setAmount(amount);
        orderBookEntry.setDelta(delta);
        orderBookEntry.setPlatform1(fullInfo1.getTradePlatform());
        orderBookEntry.setPlatform2(fullInfo2.getTradePlatform());
        orderBookEntry.setNormaliseDelta(10000 /
                fullInfo2.getSellDepth().getDepthInfoMap().firstEntry().getValue().getNormalizePrice() *
                fullInfo1.getSellDepth().getDepthInfoMap().firstEntry().getValue().getNormalizePrice() - 10000);
        return orderBookEntry;
    }

}
