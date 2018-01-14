package com.spare.cointrade.vendor.binance;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.market.OrderBook;
import com.binance.api.client.domain.market.OrderBookEntry;
import com.spare.cointrade.ExchangeContext;
import com.spare.cointrade.model.*;
import com.spare.cointrade.util.AkkaContext;
import com.spare.cointrade.util.CoinTradeConstants;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class BinanceSimpleSyncer implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(BinanceSimpleSyncer.class);

    private static ScheduledExecutorService scheduledExecutorService;

    private static AtomicBoolean started = new AtomicBoolean();

    private ActorSelection listingInfoMonitor;

    private BinanceApiRestClient client;

    public BinanceSimpleSyncer() {
        BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
        client = factory.newRestClient();
        //init usdt
        ExchangeContext.binanceCoinUsdtMap.put(CoinType.USDT, 1.0);
    }

    @PostConstruct
    public void start() {
        if(! started.compareAndSet(false, true)) {
            logger.error("Syncer has started");
            return;
        }
        listingInfoMonitor = AkkaContext.getSystem().actorSelection(
                AkkaContext.getFullActorName(CoinTradeConstants.ACTOR_LISTING_INFO_MONITOR));
        scheduledExecutorService = Executors.newScheduledThreadPool(1, new DefaultThreadFactory("BinanceSimpleSyncer"));
        scheduledExecutorService.scheduleWithFixedDelay(this, 5, 2, TimeUnit.SECONDS);

    }

    @Override
    public void run() {
        try {
            int count = 10;
            syncOneCoin(CoinType.BTC, CoinType.USDT, 5);
            syncOneCoin(CoinType.ETH, CoinType.USDT, count);
            syncOneCoin(CoinType.LTC, CoinType.USDT, count);
            syncOneCoin(CoinType.QTUM, CoinType.BTC, count);
            syncOneCoin(CoinType.EOS, CoinType.BTC, count);
            syncOneCoin(CoinType.BTG, CoinType.BTC, count);

//            syncOneCoin(CoinType.BCC, CoinType.USDT, count);
//            syncOneCoin(CoinType.NEO, CoinType.USDT, count);

        } catch (Exception e) {
            logger.error("ERROR ", e);
        } catch (Throwable e) {
            logger.error("Throw ERROR ", e);
            throw e;
        }
    }

    private void syncOneCoin(CoinType sourceCoin, CoinType targetCoin, int count) {
        try {
            OrderBook orderBook = client.getOrderBook(sourceCoin.name() + targetCoin.name(), count);
            if(targetCoin.equals(CoinType.USDT)) {
                if(orderBook.getAsks().size() > 0) {
                    //标识btc -> usdt的价格
                    ExchangeContext.binanceCoinUsdtMap.put(sourceCoin, Double.parseDouble(orderBook.getAsks().get(0).getPrice()));
                }
            }
            ListingFullInfo listingFullInfo = convert(orderBook, sourceCoin, targetCoin);
            if(listingFullInfo != null) {
                listingInfoMonitor.tell(listingFullInfo, ActorRef.noSender());
            }
        } catch (Exception e) {
            logger.error("ERROR ", e);
        }

    }

    private ListingFullInfo convert(OrderBook orderBookInfo, CoinType sourceCoin, CoinType targetCoin) {
        if(orderBookInfo == null) {
            return null;
        }
        ListingFullInfo listingFullInfo = new ListingFullInfo();
        listingFullInfo.setTradePlatform(TradePlatform.BINANCE);
        listingFullInfo.setTradeType(TradeType.COIN_COIN);
        listingFullInfo.setSourceCoinType(sourceCoin);
        listingFullInfo.setTargetCoinType(targetCoin);
        updateDepth(orderBookInfo.getAsks(), listingFullInfo.getBuyDepth(), targetCoin);
        updateDepth(orderBookInfo.getBids(), listingFullInfo.getSellDepth(), targetCoin);
        listingFullInfo.setTimestamp(orderBookInfo.getLastUpdateId());
        return listingFullInfo;
    }

    private ListingDepth updateDepth(List<OrderBookEntry> listingPairList, ListingDepth listingDepth, CoinType targetCoin) {
        if(listingPairList == null) {
            return null;
        }
        for (OrderBookEntry listingPair : listingPairList) {
            ListingDepth.DepthInfo depthInfo = listingDepth.new DepthInfo();
            depthInfo.setOriPrice(Double.parseDouble(listingPair.getPrice()));

            if(ExchangeContext.binanceCoinUsdtMap.containsKey(targetCoin)) {
                depthInfo.setNormalizePrice(ExchangeContext.normalizeToCNY(
                        CoinType.USDT, ExchangeContext.binanceCoinUsdtMap.get(targetCoin) * depthInfo.getOriPrice()));
//                depthInfo.setNormalizePrice(depthInfo.getOriPrice() * coinUsdtMap.get(targetCoin) * ExchangeContext.USD2CNY());
            } else {
                depthInfo.setNormalizePrice(0.0);
            }
            depthInfo.setAmount(Double.parseDouble(listingPair.getQty()));
            depthInfo.setTotalNormalizePrice(depthInfo.getNormalizePrice() * depthInfo.getAmount());
            listingDepth.getDepthInfoMap().put(depthInfo.getNormalizePrice(), depthInfo);
        }
        return listingDepth;
    }

    public static void main(String[] args) {
        BinanceSimpleSyncer binanceSimpleSyncer = new BinanceSimpleSyncer();
        binanceSimpleSyncer.run();
    }

}
