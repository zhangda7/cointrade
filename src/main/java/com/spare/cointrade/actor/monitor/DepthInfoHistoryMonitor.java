package com.spare.cointrade.actor.monitor;

import akka.actor.AbstractActor;
import akka.actor.Props;
import com.spare.cointrade.model.*;
import com.spare.cointrade.service.DepthInfoHistoryService;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 实时记录收到的深度信息
 * Created by dada on 2017/12/24.
 */
@Component
public class DepthInfoHistoryMonitor implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(DepthInfoHistoryMonitor.class);

//    public static Props props () {
//        return Props.create(DepthInfoHistoryMonitor.class, () -> new DepthInfoHistoryMonitor());
//    }

    private static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(
            1, new DefaultThreadFactory("depthHistoryMonitor"));

    /**
     * 存储所有平台的挂牌信息
     * key:ListingFullInfo.toKey()，包括平台。交易币种等
     */
    public static Map<String, ListingFullInfo> listingFullInfoMap = new ConcurrentHashMap<>();

//    @Override
//    public Receive createReceive() {
//        return receiveBuilder().match(ListingFullInfo.class, (listingFullInfo -> {
//            try {
////                logger.info("Receive {}", listingFullInfo);
//                String key = listingFullInfo.toKey();
//                if(listingFullInfo.getInfoStatus() != null &&
//                        listingFullInfo.getInfoStatus().equals(InfoStatus.CLEAR)) {
//                    listingFullInfoMap.remove(key);
//                    return;
//                }
//                if(! listingFullInfoMap.containsKey(key)) {
//                    listingFullInfo.setRequestTs(System.currentTimeMillis());
//                    listingFullInfoMap.put(key, listingFullInfo);
//                    return;
//                }
//                if(listingFullInfo.getTradePlatform().equals(TradePlatform.OKEX)) {
//                    updateListingInfo(listingFullInfoMap.get(key), listingFullInfo);
//                } else {
//                    clearAndSetListingInfo(listingFullInfoMap.get(key), listingFullInfo);
//                }
//
//                saveDepthInfo(listingFullInfoMap.get(key));
//
//            } catch (Exception e) {
//                logger.error("ERROR ", e);
//            }
//        })).build();
//    }

    @PostConstruct
    public void start() {
        scheduledExecutorService.scheduleWithFixedDelay(this, 10, 10, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        try {
            long sampleTs = System.currentTimeMillis();
            for (Map.Entry<String, ListingFullInfo> entry : ListingInfoMonitor.listingFullInfoMap.entrySet()) {
                saveDepthInfo(entry.getValue(), sampleTs);
            }
        } catch (Exception e) {
            logger.error("ERROR ", e);
        } catch (Throwable e) {
            logger.error("THROW ERROR ", e);
        }
    }

    private void saveDepthInfo(ListingFullInfo fullInfo, long sampleTs) {
        DepthInfoHistory depthInfoHistory = new DepthInfoHistory();
        depthInfoHistory.setPlatform(fullInfo.getTradePlatform());
        depthInfoHistory.setSourceCoin(fullInfo.getSourceCoinType());
        depthInfoHistory.setTargetCoin(fullInfo.getTargetCoinType());
        ListingDepth.DepthInfo bidDepth = fullInfo.getBuyDepth().getDepthInfoMap().firstEntry().getValue();
        depthInfoHistory.setOriBidPrice1(bidDepth.getOriPrice());
        depthInfoHistory.setNormalizeBidPrice1(bidDepth.getNormalizePrice());
        depthInfoHistory.setBidAmount1(bidDepth.getAmount());

        ListingDepth.DepthInfo askDepth = fullInfo.getSellDepth().getDepthInfoMap().firstEntry().getValue();
        depthInfoHistory.setOriAskPrice1(askDepth.getOriPrice());
        depthInfoHistory.setNormalizeAskPrice1(askDepth.getNormalizePrice());
        depthInfoHistory.setAskAmount1(askDepth.getAmount());

        depthInfoHistory.setSampleTs(sampleTs);

        DepthInfoHistoryService.INSTANCE.insert(depthInfoHistory);
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
        source.setRequestTs(System.currentTimeMillis());
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
