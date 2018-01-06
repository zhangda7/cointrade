package com.spare.cointrade.actor.monitor;

import akka.actor.AbstractActor;
import akka.actor.Props;
import com.spare.cointrade.actor.trade.TradeJudgeV2;
import com.spare.cointrade.actor.trade.TradeJudgeV3;
import com.spare.cointrade.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实时汇总接收到的信息
 * 后面可能改名字，该类收到消息，汇总结束后，再发给后面的trade模块进行交易
 * Created by dada on 2017/12/24.
 */
public class ListingInfoMonitor extends AbstractActor {

    private static Logger logger = LoggerFactory.getLogger(ListingInfoMonitor.class);

    public static Props props () {
        return Props.create(ListingInfoMonitor.class, () -> new ListingInfoMonitor());
    }

    /**
     * 存储所有平台的挂牌信息
     * key:ListingFullInfo.toKey()，包括平台。交易币种等
     */
    public static Map<String, ListingFullInfo> listingFullInfoMap = new ConcurrentHashMap<>();

    public static Map<CoinType, OrderBookEntry> chanceTradeMap = new ConcurrentHashMap<>();

    private TradeJudgeV3 tradeJudgeV3 = new TradeJudgeV3();

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(ListingFullInfo.class, (listingFullInfo -> {
            try {
//                logger.info("Receive {}", listingFullInfo);
                String key = listingFullInfo.toKey();
                if(listingFullInfo.getInfoStatus() != null &&
                        listingFullInfo.getInfoStatus().equals(InfoStatus.CLEAR)) {
                    listingFullInfoMap.remove(key);
                    return;
                }
                if(! listingFullInfoMap.containsKey(key)) {
                    listingFullInfoMap.put(key, listingFullInfo);
                    return;
                }
                if(listingFullInfo.getTradePlatform().equals(TradePlatform.OKEX)) {
                    updateListingInfo(listingFullInfoMap.get(key), listingFullInfo);
                } else if(listingFullInfo.getTradePlatform().equals(TradePlatform.HUOBI)){
                    clearAndSetListingInfo(listingFullInfoMap.get(key), listingFullInfo);
                } else if(listingFullInfo.getTradePlatform().equals(TradePlatform.BITHUMB)){
                    clearAndSetListingInfo(listingFullInfoMap.get(key), listingFullInfo);
                } else if(listingFullInfo.getTradePlatform().equals(TradePlatform.BINANCE)){
                    clearAndSetListingInfo(listingFullInfoMap.get(key), listingFullInfo);
                }

                tradeJudgeV3.findTradeChance();
                //检查是否可以交易
//                checkTradeChance();
//                judgeClearCache();
//                parseHuobi(depth);
            } catch (Exception e) {
                logger.error("ERROR ", e);
            }
        })).build();
    }

    private void calcOneDelta() {
        List<ListingFullInfo> targetChain = findOneChain(new CoinType[] {CoinType.USDT, CoinType.BTG, CoinType.BTC, CoinType.USDT});

    }

    private List<ListingFullInfo> findOneChain(CoinType[] targetChain) {
//        CoinType[] targetChain = new CoinType[] {CoinType.USDT, CoinType.BTG, CoinType.BTC, CoinType.USDT};
        List<ListingFullInfo> targetList = new ArrayList<>();
        while (true) {
            int index = 0;

            for(ListingFullInfo listingFullInfo : listingFullInfoMap.values()) {
                if(listingFullInfo.getSourceCoinType().equals(targetChain[index]) &&
                        listingFullInfo.getTargetCoinType().equals(targetChain[index + 1])) {
                    targetList.add(listingFullInfo);

                    break;
                }
            }
            index ++;

            if(index >= 3) {
                break;
            }
        }
        return targetList;
    }



    /**
     * 根据传递的更新的target info，更新source info
     * @param source
     * @param target
     */
    private void updateListingInfo(ListingFullInfo source, ListingFullInfo target) {
        if(target.getBuyDepth() != null) {
            updateOneDepth(source.getBuyDepth(), target.getBuyDepth());
        }
        if(target.getSellDepth() != null) {
            updateOneDepth(source.getSellDepth(), target.getSellDepth());
        }
    }

    /**
     * 全部赋值最新的
     * @param source
     * @param target
     */
    private void clearAndSetListingInfo(ListingFullInfo source, ListingFullInfo target) {
        source.setTimestamp(target.getTimestamp());
        if(target.getBuyDepth() != null) {
            source.getBuyDepth().getDepthInfoMap().clear();
            updateOneDepth(source.getBuyDepth(), target.getBuyDepth());
        }
        if(target.getSellDepth() != null) {
            source.getSellDepth().getDepthInfoMap().clear();
            updateOneDepth(source.getSellDepth(), target.getSellDepth());
        }
    }

    private void updateOneDepth(ListingDepth source, ListingDepth target) {
        if(target.getDepthInfoMap() == null) {
            return;
        }
        for (ListingDepth.DepthInfo depthInfo : target.getDepthInfoMap().values()) {
            //hard code double precision
            if(depthInfo.getAmount() == 0) {
                source.getDepthInfoMap().remove(depthInfo.getOriPrice());
                continue;
            }
            source.getDepthInfoMap().put(depthInfo.getOriPrice(), depthInfo);
        }
    }

}
