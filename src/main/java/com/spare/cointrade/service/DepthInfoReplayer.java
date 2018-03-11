package com.spare.cointrade.service;

import akka.AkkaException;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import com.spare.cointrade.ExchangeContext;
import com.spare.cointrade.model.*;
import com.spare.cointrade.util.AkkaContext;
import com.spare.cointrade.util.CoinTradeConstants;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class DepthInfoReplayer implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(DepthInfoReplayer.class);

    private static AtomicBoolean started = new AtomicBoolean();

    private static ExecutorService executorService = Executors.newSingleThreadExecutor(new DefaultThreadFactory("DepthInfoReplaer"));

    private ActorSelection listingInfoMonitor;

    private Long startTs = null;

    private Long endTs = null;

    @PostConstruct
    public void start() throws ParseException {
        if(! started.compareAndSet(false, true)) {
            logger.warn("DepthInfoReplayer has already started, just return");
            return;
        }
        listingInfoMonitor = AkkaContext.getSystem().actorSelection(
                AkkaContext.getFullActorName(CoinTradeConstants.ACTOR_LISTING_INFO_MONITOR));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        startTs = sdf.parse("2018-03-01 00:00:00").getTime();
        endTs = sdf.parse("2018-03-02 00:00:00").getTime();
        executorService.execute(this);
    }

    @Override
    public void run() {
        try {
            Long curTs = startTs;

            int step = 60 * 1000 * 5;
            while (curTs <= endTs) {
                List<DepthInfoHistory> depthInfoHistoryList = DepthInfoHistoryService.INSTANCE.listByTs(curTs, curTs + step);
                for(DepthInfoHistory depthInfoHistory : depthInfoHistoryList) {
                    ListingFullInfo fullInfo = convert(depthInfoHistory);
                    if(depthInfoHistory.getPlatform().equals(TradePlatform.BINANCE) && depthInfoHistory.getTargetCoin().equals(CoinType.USDT)) {
                        //标识BTC->USDT的价格
                        ExchangeContext.binanceCoinUsdtMap.put(depthInfoHistory.getSourceCoin(), depthInfoHistory.getOriAskPrice1());
                    }
                    if(fullInfo != null) {
                        listingInfoMonitor.tell(fullInfo, ActorRef.noSender());
                    }
                }
                //每次多加1，保证数据不重复
                curTs += step + 1;
            }
        } catch (Exception e) {
            logger.error("ERROR ", e);
        } catch (Throwable e) {
            logger.error("THROW ERROR ", e);
            throw e;
        }
    }

    private ListingFullInfo convert(DepthInfoHistory depthInfoHistory) {
        if(depthInfoHistory == null) {
            return null;
        }
        ListingFullInfo listingFullInfo = new ListingFullInfo();
        listingFullInfo.setTradePlatform(depthInfoHistory.getPlatform());
        listingFullInfo.setTradeType(TradeType.COIN_COIN);
        listingFullInfo.setSourceCoinType(depthInfoHistory.getSourceCoin());
        listingFullInfo.setTargetCoinType(depthInfoHistory.getTargetCoin());

        addBuyDepth(listingFullInfo.getBuyDepth(), depthInfoHistory);
        addSellDepth(listingFullInfo.getSellDepth(), depthInfoHistory);

        listingFullInfo.setTimestamp(depthInfoHistory.getSampleTs());
        return listingFullInfo;
    }

    private void addBuyDepth(ListingDepth buydepth, DepthInfoHistory depthInfoHistory) {
        ListingDepth.DepthInfo depthInfo = buydepth.new DepthInfo();
        depthInfo.setAmount(depthInfoHistory.getBidAmount1());
        depthInfo.setOriPrice(depthInfoHistory.getOriBidPrice1());
        depthInfo.setNormalizePrice(depthInfoHistory.getNormalizeBidPrice1());
        buydepth.getDepthInfoMap().put(depthInfo.getNormalizePrice(), depthInfo);
    }

    private void addSellDepth(ListingDepth selldepth, DepthInfoHistory depthInfoHistory) {
        ListingDepth.DepthInfo depthInfo = selldepth.new DepthInfo();
        depthInfo.setAmount(depthInfoHistory.getAskAmount1());
        depthInfo.setOriPrice(depthInfoHistory.getOriAskPrice1());
        depthInfo.setNormalizePrice(depthInfoHistory.getNormalizeAskPrice1());
        selldepth.getDepthInfoMap().put(depthInfo.getNormalizePrice(), depthInfo);
    }
}
