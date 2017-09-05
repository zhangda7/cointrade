package com.spare.cointrade.actor.trade;

import akka.actor.AbstractActor;
import akka.actor.Props;
import com.spare.cointrade.actor.minitor.HuobiTradeMonitor;
import com.spare.cointrade.actor.minitor.OkCoinTradeMonitor;
import com.spare.cointrade.model.TradeAction;
import com.spare.cointrade.model.TradeInfo;
import com.spare.cointrade.model.trade.HuobiTrade;
import com.spare.cointrade.model.trade.OkCoinTrade;
import com.spare.cointrade.trade.huobi.ApiException;
import com.spare.cointrade.trade.huobi.HuobiTradeClient;
import com.spare.cointrade.trade.okcoin.OkCoinTradeClient;
import com.spare.cointrade.util.ApplicationContextHolder;
import com.spare.cointrade.util.CoinTradeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 执行实际交易的actor
 * Created by dada on 2017/8/20.
 */
public class OkCoinTrader extends AbstractActor {

    private static Logger logger = LoggerFactory.getLogger(OkCoinTrader.class);

    public static Props props() {
        return Props.create(OkCoinTrader.class, () -> new OkCoinTrader());
    }

    private OkCoinTradeClient okCoinTradeClient;

    private AtomicInteger tradeCount = new AtomicInteger(0);

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder().match(TradeInfo.class, trade -> {
            if(okCoinTradeClient == null) {
                okCoinTradeClient = ApplicationContextHolder.getBean(OkCoinTradeClient.class);
            }

            if(okCoinTradeClient == null) {
                logger.error("ERROR on init huobiTradeClient");
                return;
            }
            try{
                logger.info("Receive ok coin trade {}", trade);
                if(!CoinTradeContext.DO_TRADE) {
                    logger.info("OK coin trade status is {}, return", CoinTradeContext.DO_TRADE);
                    return;
                }
                if(CoinTradeContext.MAX_TRADE_COUNT > 0 && tradeCount.getAndIncrement() >= CoinTradeContext.MAX_TRADE_COUNT) {
                    logger.info("OK coin reach max count {}, return", tradeCount.get());
                    return;
                }
                String orderId = okCoinTradeClient.createEthOrder(trade.getPrice(), trade.getAmount(), trade.getAction());
                trade.setOrderId(orderId);
                logger.info("Order is is {} for {}", orderId, trade);
//                OkCoinTradeMonitor.getTobeConfirmedTradeQueue().add(trade);
            } catch (ApiException e) {
                trade.setAction(TradeAction.FAIL);
                trade.setComment(e.getMessage());
//                OkCoinTradeMonitor.getTobeConfirmedTradeQueue().add(trade);
            }
            catch (Exception e) {
                logger.error("ERROR on handle {}", trade, e);
            }
        }).build();
    }

}
