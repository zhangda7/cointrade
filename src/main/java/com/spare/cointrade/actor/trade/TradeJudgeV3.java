package com.spare.cointrade.actor.trade;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import com.alibaba.fastjson.JSON;
import com.spare.cointrade.ExchangeContext;
import com.spare.cointrade.actor.monitor.ListingInfoMonitor;
import com.spare.cointrade.log.TradeChanceLog;
import com.spare.cointrade.model.*;
import com.spare.cointrade.service.TradeHistoryService;
import com.spare.cointrade.account.AccountManager;
import com.spare.cointrade.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class TradeJudgeV3 {

    private static Logger logger = LoggerFactory.getLogger(TradeJudgeV3.class);

    private static Logger tradeChanceLogger = LoggerFactory.getLogger(TradeChanceLog.class);

    private ActorSelection tradeStateSyncer;

    private static final Double MIN_TRADE_AMOUNT = 0.01;

    public static Map<String, OrderBookEntry> chanceTradeMap = new ConcurrentHashMap<>();

    private static AtomicLong pairIdGenerator = new AtomicLong();

    private static boolean canTrade = true;

    private Map<CoinType, Double> minCoinTradeAmountMap = new HashMap<>();

    public static void setCanTrade(boolean canTrade) {
        TradeJudgeV3.canTrade = canTrade;
    }

    private static final Double REVERSE_AVERAGE_NORMALIZE_PERCENT = 0.95;

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

    private static final Double DECREASE_PERCENT = 0.8;

    private void doTrade(TradePair tradePair) {
        if(! canTrade) {
            tradeChanceLogger.info("Status of canTrade is {}, just return", canTrade);
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
        TradeConfigContext.getINSTANCE().addProfit(profit);
        mockDoTrade(tradePair, tradePair.getTradePair_1(), profit);
        mockDoTrade(tradePair, tradePair.getTradePair_2(), profit);
        mockDoTrade(tradePair, tradePair.getTradePair_3(), profit);
        OrderBookHistory orderBookHistory = TradeConfigContext.getINSTANCE().getOrderBookHistory(tradePair.getTradePair_1().getSourceCoin());
        double preTotalProfit = orderBookHistory.getTotalProfit();
        double preAmount = orderBookHistory.getTotalAmount();
        double preAverageProfit = orderBookHistory.getAverageProfit();
        if(tradePair.getTradeDirection().equals(TradeDirection.FORWARD)) {
            TradeConfigContext.getINSTANCE().updateOrderBookHistory(tradePair.getTradePair_1().getSourceCoin(),
                    tradePair.getNormalizePriceDelta(), tradePair.getTradePair_1().getAmount());
        } else if(tradePair.getTradeDirection().equals(TradeDirection.REVERSE)) {
            TradeConfigContext.getINSTANCE().updateOrderBookHistory(tradePair.getTradePair_1().getSourceCoin(),
                    tradePair.getNormalizePriceDelta() * -1, tradePair.getTradePair_1().getAmount());
        } else {
            throw new IllegalArgumentException("Trade direction is wrong:" + tradePair.getTradeDirection());
        }
        logger.info("Normalise profit change [{} {} {}] -> [{} {} {}]", preTotalProfit, preAmount, preAverageProfit,
                orderBookHistory.getTotalProfit(), orderBookHistory.getTotalAmount(), orderBookHistory.getAverageProfit());
        canTrade = false;
        this.tradeStateSyncer.tell(tradePair, ActorRef.noSender());
    }

    private void mockDoTrade(TradePair tradePair, SignalTrade signalTrade, double profit) {
        if(signalTrade == null) {
            return;
        }
        TradeHistory tradeHistory = new TradeHistory();
        tradeHistory.setPairId(tradePair.getPairId());
        tradeHistory.setDirection(tradePair.getTradeDirection().name());
        tradeHistory.setTradePlatform(signalTrade.getTradePlatform());
        tradeHistory.setTradeAction(signalTrade.getTradeAction());
        tradeHistory.setCoinType(signalTrade.getSourceCoin());
        tradeHistory.setTargetCoinType(signalTrade.getTargetCoin());
        tradeHistory.setPrice(signalTrade.getPrice());
        tradeHistory.setAmount(signalTrade.getAmount());
        tradeHistory.setResult(signalTrade.getResult());
        tradeHistory.setProfit(profit);
        Account account = AccountManager.INSTANCE.getPlatformAccountMap().get(signalTrade.getTradePlatform());
        Balance sourceBalance = account.getBalanceMap().get(signalTrade.getSourceCoin());
        Balance targetBalance = null;
        if(signalTrade.getTargetCoin().equals(CoinType.KRW) ||
                signalTrade.getTargetCoin().equals(CoinType.CNY)) {
            targetBalance = account.getMoneyBalance();
            tradeHistory.setNormalizePrice(ExchangeContext.normalizeToUSD(signalTrade.getTargetCoin(), signalTrade.getPrice()));
        } else {
            targetBalance = account.getBalanceMap().get(signalTrade.getTargetCoin());
            tradeHistory.setNormalizePrice(
                    ExchangeContext.normalizeToUSD(CoinType.USDT,
                            signalTrade.getPrice() * ExchangeContext.currency2USDT(
                                    signalTrade.getTradePlatform(), signalTrade.getTargetCoin())));
        }
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
        logger.info("Prepare insert tradeHistory {}", JSON.toJSONString(tradeHistory));
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

        if(tradePairList.size() > 0) {
            //如果当前存在正向的交易，则反向交易不做
            return;
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
//            OrderBookHistory orderBookHistory = TradeConfigContext.getINSTANCE().getOrderBookHistory(orderBookEntry.getCoinType());
//            if(Math.abs(orderBookEntry.getNormaliseDelta()) > orderBookHistory.getAverageProfit() * REVERSE_AVERAGE_NORMALIZE_PERCENT) {
//                continue;
//            }
            for(Map.Entry<CoinType,OrderBookHistory> entry : TradeConfigContext.getINSTANCE().getOrderBookHistoryMap().entrySet()) {
                OrderBookHistory orderBookHistory = entry.getValue();
                if(Math.abs(orderBookEntry.getNormaliseDelta()) > orderBookHistory.getAverageProfit() * REVERSE_AVERAGE_NORMALIZE_PERCENT) {
                    continue;
                }
                //TODO 再这里增加更详细的判断 how?
                tradeChanceLogger.info("Can do reverse trade, cur delta {}, target average profit {}",
                        orderBookEntry.getNormaliseDelta(), orderBookHistory.getAverageProfit(), JSON.toJSONString(orderBookEntry));
                TradePair maxDeltaPair = createReverseTradePair(orderBookEntry);
                if(maxDeltaPair == null) {
                    continue;
                }
                tradePairList.add(maxDeltaPair);
                //一次最多返回一次reverse交易
                return tradePairList;
            }
        }
        return tradePairList;
    }

    private Map<Double, CoinType> sortOrderBookHistoryMap(Map<CoinType, OrderBookHistory> orderBookHistoryMap) {
        if(orderBookHistoryMap == null) {
            return null;
        }
        TreeMap<Double, CoinType> sorted = new TreeMap<>();
        for (Map.Entry<CoinType,OrderBookHistory> entry : orderBookHistoryMap.entrySet()) {
            sorted.put(entry.getValue().getAverageProfit(), entry.getKey());
        }
        return sorted;
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
            //1 = 高价平台，应卖出
            //2 = 低价平台，应买进
            return judgeAndMakePair(fullInfo2, fullInfo1, maxEntry, TradeDirection.FORWARD);
        } else {
            return judgeAndMakePair(fullInfo1, fullInfo2, maxEntry, TradeDirection.FORWARD);
        }

    }

    /**
     *
     * @param buySide 要买进（价格低）的平台，需查询sell 价
     * @param sellSide 要卖出（价格高）的平台，需查询buy 价
     * @param orderBookEntry
     * @param tradeDirection
     * @return
     */
    private TradePair judgeAndMakePair(ListingFullInfo buySide, ListingFullInfo sellSide,
                                       OrderBookEntry orderBookEntry, TradeDirection tradeDirection) {
        TradePair tradePair = new TradePair();
        tradePair.setTradeDirection(tradeDirection);
        tradePair.setNormalizePriceDelta(orderBookEntry.getNormaliseDelta());
        double maxSellAmount = Math.min(ListingDepthUtil.getLevelDepthInfo(sellSide.getBuyDepth(), MONITOR_DEPTH_LEVEL).getAmount(),
                AccountManager.INSTANCE.getFreeAmount(sellSide.getTradePlatform(), orderBookEntry.getCoinType()));

        ListingDepth.DepthInfo sellDepth = ListingDepthUtil.getLevelDepthInfo(buySide.getSellDepth(), MONITOR_DEPTH_LEVEL);

        double maxBuyAmount = AccountManager.INSTANCE.getFreeAmount(
                buySide.getTradePlatform(), buySide.getTargetCoinType()) / sellDepth.getOriPrice();
//        double maxDepthBuyAmount = sellDepth.getAmount() * sellDepth.getOriPrice();
//        double maxBuyAmount = Math.min(ListingDepthUtil.getLevelDepthInfo(buySide.getSellDepth(), MONITOR_DEPTH_LEVEL).getAmount(),
//                AccountManager.INSTANCE.getFreeAmount(buySide.getTradePlatform(), buySide.getTargetCoinType()));

        double minAmount = Math.min(maxSellAmount, maxBuyAmount);

        Double coinMinTradeAmount = minCoinTradeAmountMap.get(buySide.getSourceCoinType());
        if(coinMinTradeAmount == null) {
            coinMinTradeAmount = MIN_TRADE_AMOUNT;
        }

        if(tradeDirection.equals(TradeDirection.REVERSE)) {
            /**
             * 把总收益的百分比也加上，反转时不能超出这个阈值
             */
            double maxReverseMoney = TradeConfigContext.getINSTANCE().getTotalProfit() * REVERSE_TOTAL_NORLAIZE_PERCENT;

            //暂时使用买方的归一化价格来计算最小交易数量
//        double minAmount2 = maxReverseMoney / buySide.getBuyDepth().getDepthInfoMap().firstEntry().getValue().getNormalizePrice();
            double minAmount2 = maxReverseMoney / ListingDepthUtil.getLevelDepthInfo(buySide.getBuyDepth(), MONITOR_DEPTH_LEVEL).getNormalizePrice();

            minAmount = Math.min(minAmount, minAmount2);
        }

        minAmount = Math.min(minAmount, TradeConfigContext.getINSTANCE().getMaxTradeAmount(orderBookEntry.getCoinType()));

        if(minAmount < coinMinTradeAmount) {
            tradeChanceLogger.info("Min amount is {} < {}, just return [{} {} {}] [{} {} {}]",
                    minAmount, coinMinTradeAmount, buySide.getTradePlatform(), buySide.getSourceCoinType(), buySide.getTargetCoinType(),
                    sellSide.getTradePlatform(), sellSide.getSourceCoinType(), sellSide.getTargetCoinType());
            return null;
        }

        //TODO 添加手续费的判断，计算每个平台的交易数量
        //手续费的基本扣除：扣除收取到的资产
        //买的数量不动
        Double buyAmount = minAmount;

        //sell时，卖出固定的买的时候实际收到的资产
        Double sellAmount = minAmount * (1 - TradingFeesUtil.getTradeFee(buySide.getTradePlatform()));

        ListingDepth.DepthInfo sellInfo = ListingDepthUtil.getLevelDepthInfo(sellSide.getBuyDepth(), MONITOR_DEPTH_LEVEL);
        tradePair.setTradePair_1(makeOneTrade(sellSide.getTradePlatform(),
                orderBookEntry.getCoinType(), sellSide.getTargetCoinType(), // 应该为buySide.targetCoinType
                TradeAction.SELL,
                sellInfo.getOriPrice(), sellAmount,
                sellInfo.getNormalizePrice()));

        ListingDepth.DepthInfo buyInfo = ListingDepthUtil.getLevelDepthInfo(buySide.getSellDepth(), MONITOR_DEPTH_LEVEL);
        tradePair.setTradePair_2(makeOneTrade(buySide.getTradePlatform(),
                orderBookEntry.getCoinType(), buySide.getTargetCoinType(),
                TradeAction.BUY,
                buyInfo.getOriPrice(), buyAmount,
                buyInfo.getNormalizePrice()));

        //TODO 增加BTC的关联交易
        try {
            SignalTrade btcTrade = balanceBinanceBTC(tradePair.getTradePair_1());
            if(btcTrade != null) {
                tradePair.setTradePair_3(btcTrade);
            } else {
                btcTrade = balanceBinanceBTC(tradePair.getTradePair_2());
                if(btcTrade != null) {
                    tradePair.setTradePair_3(btcTrade);
                }
            }
        } catch (Exception e) {
            logger.error("ERROR on balance btc {}", e.getMessage());
            return null;
        }

        return tradePair;
    }

//    /**
//     * 判断是否存在反向交易
//     * 1.如果能执行到这个函数，说明目前已经没有正向交易可以执行
//     * 2.
//     * @param buySide 要买进（价格低）的平台，需查询sell 价
//     * @param sellSide 要卖出（价格高）的平台，需查询buy 价
//     * @param orderBookEntry
//     * @return
//     */
//    private TradePair judgeAndMakeReversePair(ListingFullInfo buySide, ListingFullInfo sellSide,
//                                       OrderBookEntry orderBookEntry) {
//        TradePair tradePair = new TradePair();
//        tradePair.setTradeDirection(TradeDirection.REVERSE);
//        tradePair.setNormalizePriceDelta(orderBookEntry.getNormaliseDelta());
//        double maxSellAmount = Math.min(ListingDepthUtil.getLevelDepthInfo(sellSide.getBuyDepth(), MONITOR_DEPTH_LEVEL).getAmount(),
//                AccountManager.INSTANCE.getFreeAmount(sellSide.getTradePlatform(), orderBookEntry.getCoinType()));
//
//        ListingDepth.DepthInfo sellDepth = ListingDepthUtil.getLevelDepthInfo(buySide.getSellDepth(), MONITOR_DEPTH_LEVEL);
//
//        double maxBuyAmount = AccountManager.INSTANCE.getFreeAmount(
//                buySide.getTradePlatform(), buySide.getTargetCoinType()) / sellDepth.getOriPrice();
////        double maxDepthBuyAmount = sellDepth.getAmount() * sellDepth.getOriPrice();
////        double maxBuyAmount = Math.min(ListingDepthUtil.getLevelDepthInfo(buySide.getSellDepth(), MONITOR_DEPTH_LEVEL).getAmount(),
////                AccountManager.INSTANCE.getFreeAmount(buySide.getTradePlatform(), buySide.getTargetCoinType()));
//
//        double minAmount = Math.min(maxSellAmount, maxBuyAmount);
//
//        Double coinMinTradeAmount = minCoinTradeAmountMap.get(buySide.getSourceCoinType());
//        if(coinMinTradeAmount == null) {
//            coinMinTradeAmount = MIN_TRADE_AMOUNT;
//        }
//
////        if(tradeDirection.equals(TradeDirection.REVERSE)) {
//            /**
//             * 把总收益的百分比也加上，反转时不能超出这个阈值
//             */
//            double maxReverseMoney = TradeConfigContext.getINSTANCE().getTotalProfit() * REVERSE_TOTAL_NORLAIZE_PERCENT;
//
//            //暂时使用买方的归一化价格来计算最小交易数量
////        double minAmount2 = maxReverseMoney / buySide.getBuyDepth().getDepthInfoMap().firstEntry().getValue().getNormalizePrice();
//            double minAmount2 = maxReverseMoney / ListingDepthUtil.getLevelDepthInfo(buySide.getBuyDepth(), MONITOR_DEPTH_LEVEL).getNormalizePrice();
//
//            minAmount = Math.min(minAmount, minAmount2);
////        }
//
//        minAmount = Math.min(minAmount, TradeConfigContext.getINSTANCE().getMaxTradeAmount(orderBookEntry.getCoinType()));
//
//        if(minAmount < coinMinTradeAmount) {
//            tradeChanceLogger.info("Min amount is {} < {}, just return [{} {} {}] [{} {} {}]",
//                    minAmount, coinMinTradeAmount, buySide.getTradePlatform(), buySide.getSourceCoinType(), buySide.getTargetCoinType(),
//                    sellSide.getTradePlatform(), sellSide.getSourceCoinType(), sellSide.getTargetCoinType());
//            return null;
//        }
//
//        //TODO 添加手续费的判断，计算每个平台的交易数量
//        //手续费的基本扣除：扣除收取到的资产
//        //买的数量不动
//        Double buyAmount = minAmount;
//
//        //sell时，卖出固定的买的时候实际收到的资产
//        Double sellAmount = minAmount * (1 - TradingFeesUtil.getTradeFee(buySide.getTradePlatform()));
//
//        ListingDepth.DepthInfo sellInfo = ListingDepthUtil.getLevelDepthInfo(sellSide.getBuyDepth(), MONITOR_DEPTH_LEVEL);
//        tradePair.setTradePair_1(makeOneTrade(sellSide.getTradePlatform(),
//                orderBookEntry.getCoinType(), sellSide.getTargetCoinType(), // 应该为buySide.targetCoinType
//                TradeAction.SELL,
//                sellInfo.getOriPrice(), sellAmount,
//                sellInfo.getNormalizePrice()));
//
//        ListingDepth.DepthInfo buyInfo = ListingDepthUtil.getLevelDepthInfo(buySide.getSellDepth(), MONITOR_DEPTH_LEVEL);
//        tradePair.setTradePair_2(makeOneTrade(buySide.getTradePlatform(),
//                orderBookEntry.getCoinType(), buySide.getTargetCoinType(),
//                TradeAction.BUY,
//                buyInfo.getOriPrice(), buyAmount,
//                buyInfo.getNormalizePrice()));
//
//        //TODO 增加BTC的关联交易
//        try {
//            SignalTrade btcTrade = balanceBinanceBTC(tradePair.getTradePair_1());
//            if(btcTrade != null) {
////            tradePair.setTradePair_3(btcTrade);
//            } else {
//                btcTrade = balanceBinanceBTC(tradePair.getTradePair_2());
//                if(btcTrade != null) {
////                tradePair.setTradePair_3(btcTrade);
//                }
//            }
//        } catch (Exception e) {
//            logger.error("ERROR on balance btc", e);
//        }
//
//        return tradePair;
//    }

    private SignalTrade balanceBinanceBTC(SignalTrade signalTrade) {
        if(! signalTrade.getTradePlatform().equals(TradePlatform.BINANCE)) {
            return null;
        }
        if(! signalTrade.getTargetCoin().equals(CoinType.BTC)) {
            return null;
        }
        SignalTrade btcTrade = null;
        Double costBtc = signalTrade.getPrice() * signalTrade.getAmount();
        ListingFullInfo btcFullInfo = ListingInfoMonitor.listingFullInfoMap.get(
                toiListingInfoKey(signalTrade.getTradePlatform(), CoinType.BTC));

        if(signalTrade.getTradeAction().equals(TradeAction.BUY)) {
            //cost btc,need buy btc
            ListingDepth.DepthInfo depthInfo = ListingDepthUtil.getLevelDepthInfo(btcFullInfo.getSellDepth(), MONITOR_DEPTH_LEVEL);
            double maxBuyAmount = AccountManager.INSTANCE.getFreeAmount(
                    signalTrade.getTradePlatform(), CoinType.USDT) / depthInfo.getOriPrice();
            if(maxBuyAmount < costBtc) {
                throw new RuntimeException("Not enough USDT to buy " + costBtc + "BTC");
            }
            btcTrade = makeOneTrade(signalTrade.getTradePlatform(),
                CoinType.BTC, CoinType.USDT,
                TradeAction.BUY,
                depthInfo.getOriPrice(), costBtc,
                depthInfo.getNormalizePrice());
        } else if(signalTrade.getTradeAction().equals(TradeAction.SELL)){
            // need sell btc
            ListingDepth.DepthInfo depthInfo = ListingDepthUtil.getLevelDepthInfo(btcFullInfo.getBuyDepth(), MONITOR_DEPTH_LEVEL);
            Double curBtcCount = AccountManager.INSTANCE.getFreeAmount(
                    signalTrade.getTradePlatform(), CoinType.BTC);
            if(curBtcCount < costBtc) {
                throw new RuntimeException("Not enough BTC to sell " + costBtc + "BTC");
            }
            btcTrade = makeOneTrade(signalTrade.getTradePlatform(),
                    CoinType.BTC, CoinType.USDT,
                    TradeAction.SELL,
                    depthInfo.getOriPrice(), costBtc,
                    depthInfo.getNormalizePrice());
        }
        //这个关联交易还是比较复杂的，如果要买入BTC的话，还要检查USDT的钱够不够

        return btcTrade;
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
        signalTrade.setResult(TradeResult.TRADING);
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

    /**
     * 根据传入的coin type，检查所有交易平台间的差价
     * @param coinType cointype
     * @param fullInfoMap fullinfoMap 所有平台该币种的信息
     * @return
     */
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

        OrderBookEntry orderBookEntry = new OrderBookEntry();
        orderBookEntry.setCoinType(coinType);
        orderBookEntry.setAmount(amount);
        orderBookEntry.setDelta(Math.abs(delta));
        if(delta >= 0) {
            orderBookEntry.setPlatform1(fullInfo1.getTradePlatform());
            orderBookEntry.setPlatform2(fullInfo2.getTradePlatform());
            orderBookEntry.setNormaliseDelta(10000 /
                    fullSellInfo_2.getNormalizePrice() *
                    fullSellInfo_1.getNormalizePrice() - 10000);
        } else {
            orderBookEntry.setPlatform1(fullInfo2.getTradePlatform());
            orderBookEntry.setPlatform2(fullInfo1.getTradePlatform());
            orderBookEntry.setNormaliseDelta(10000 /
                    fullSellInfo_1.getNormalizePrice() *
                    fullSellInfo_2.getNormalizePrice() - 10000);
        }

        return orderBookEntry;
    }

}
