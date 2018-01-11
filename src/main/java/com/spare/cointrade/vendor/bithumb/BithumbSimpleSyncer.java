package com.spare.cointrade.vendor.bithumb;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.spare.cointrade.ExchangeContext;
import com.spare.cointrade.model.*;
import com.spare.cointrade.util.AkkaContext;
import com.spare.cointrade.util.CoinTradeConstants;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class BithumbSimpleSyncer implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(BithumbSimpleSyncer.class);

    private static ScheduledExecutorService scheduledExecutorService;

    private static AtomicBoolean started = new AtomicBoolean();

    private ActorSelection listingInfoMonitor;

    private Api_Client apiClient;
    @PostConstruct
    public void start() {
        if(! started.compareAndSet(false, true)) {
            logger.error("Syncer has started");
            return;
        }
        apiClient = new Api_Client("api connect key",
                "api secret key");
        listingInfoMonitor = AkkaContext.getSystem().actorSelection(
                AkkaContext.getFullActorName(CoinTradeConstants.ACTOR_LISTING_INFO_MONITOR));
        scheduledExecutorService = Executors.newScheduledThreadPool(1, new DefaultThreadFactory("BithumbSimpleSyncer"));
        scheduledExecutorService.scheduleWithFixedDelay(this, 5, 2, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        try {
            syncOneCoin(CoinType.BTC);
            syncOneCoin(CoinType.ETH);
            syncOneCoin(CoinType.LTC);
            syncOneCoin(CoinType.QTUM);
            syncOneCoin(CoinType.EOS);
            syncOneCoin(CoinType.BTG);

//            HashMap<String, String> rgParams = new HashMap<String, String>();
//            String result = api.callApi("/public/ticker/BTG", rgParams);
//            System.out.println(result);
        } catch (Exception e) {
            logger.error("ERROR ", e);
        } catch (Throwable e) {
            logger.error("Throw ERROR ", e);
            throw e;
        }
    }

    private void syncOneCoin(CoinType coinType) {
//        String result = apiClient.fetchDepthInfo(CoinType.BTC.name());
        String result = apiClient.fetchDepthInfo(coinType.name());
        OrderBookInfo orderBookInfo = parseOrderBook(result);
        ListingFullInfo listingFullInfo = convert(orderBookInfo);
        if(listingFullInfo != null) {
            listingInfoMonitor.tell(listingFullInfo, ActorRef.noSender());
        }
    }

    private ListingFullInfo convert(OrderBookInfo orderBookInfo) {
        if(orderBookInfo == null) {
            return null;
        }
        ListingFullInfo listingFullInfo = new ListingFullInfo();
        listingFullInfo.setTradePlatform(TradePlatform.BITHUMB);
        listingFullInfo.setTradeType(TradeType.COIN_COIN);
        listingFullInfo.setSourceCoinType(CoinType.valueOf(orderBookInfo.getOrder_currency()));
        listingFullInfo.setTargetCoinType(CoinType.valueOf(orderBookInfo.getPayment_currency()));
        updateDepth(orderBookInfo.getAsks(), listingFullInfo.getBuyDepth());
        updateDepth(orderBookInfo.getBids(), listingFullInfo.getSellDepth());
        listingFullInfo.setTimestamp(orderBookInfo.getTimestamp());
        return listingFullInfo;
    }

    private ListingDepth updateDepth(List<OrderBookInfo.ListingPair> listingPairList, ListingDepth listingDepth) {
        if(listingPairList == null) {
            return null;
        }
        for (OrderBookInfo.ListingPair listingPair : listingPairList) {
            ListingDepth.DepthInfo depthInfo = listingDepth.new DepthInfo();
            depthInfo.setOriPrice(listingPair.getPrice());
            depthInfo.setNormalizePrice(listingPair.getPrice() * ExchangeContext.KRW2CNY());
            depthInfo.setAmount(listingPair.getQuantity());
            depthInfo.setTotalNormalizePrice(depthInfo.getNormalizePrice() * depthInfo.getAmount());
            listingDepth.getDepthInfoMap().put(depthInfo.getNormalizePrice(), depthInfo);
        }
        return listingDepth;
    }

    private static OrderBookInfo parseOrderBook(String result) {
        JSONObject jsonObject = JSON.parseObject(result);
        if(! jsonObject.getString("status").equals("0000")) {
            return null;
        }
        OrderBookInfo orderBookInfo = JSON.parseObject(jsonObject.getString("data"), OrderBookInfo.class);
        return orderBookInfo;
    }
}
