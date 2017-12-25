package com.spare.cointrade.actor.monitor;

import akka.actor.AbstractActor;
import akka.actor.Props;
import com.spare.cointrade.actor.trade.TradeJudgeV2;
import com.spare.cointrade.model.InfoStatus;
import com.spare.cointrade.model.ListingDepth;
import com.spare.cointrade.model.ListingFullInfo;
import com.spare.cointrade.model.TradePlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public static Map<String, ListingFullInfo> listingFullInfoMap = new ConcurrentHashMap<>();

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(ListingFullInfo.class, (listingFullInfo -> {
            try {
                logger.info("Receive {}", listingFullInfo);
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
                updateListingInfo(listingFullInfoMap.get(key), listingFullInfo);

//                judgeClearCache();
//                parseHuobi(depth);
            } catch (Exception e) {
                logger.error("ERROR ", e);
            }
        })).build();
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

    private void updateOneDepth(ListingDepth source, ListingDepth target) {
        if(target.getDepthInfoMap() == null) {
            return;
        }
        for (ListingDepth.DepthInfo depthInfo : target.getDepthInfoMap().values()) {
            //hard code double precision
            if(depthInfo.getAmount() == 0) {
                source.getDepthInfoMap().remove(depthInfo.getPrice());
                continue;
            }
            source.getDepthInfoMap().put(depthInfo.getPrice(), depthInfo);
        }
    }

}
