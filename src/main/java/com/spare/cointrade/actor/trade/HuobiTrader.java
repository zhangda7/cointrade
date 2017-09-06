package com.spare.cointrade.actor.trade;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import com.spare.cointrade.actor.minitor.HuobiTradeMonitor;
import com.spare.cointrade.model.TradeAction;
import com.spare.cointrade.model.TradeInfo;
import com.spare.cointrade.model.trade.HuobiTrade;
import com.spare.cointrade.trade.huobi.ApiException;
import com.spare.cointrade.trade.huobi.HuobiTradeClient;
import com.spare.cointrade.util.AkkaContext;
import com.spare.cointrade.util.ApplicationContextHolder;
import com.spare.cointrade.util.CoinTradeContext;
import com.spare.cointrade.util.SpringExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.App;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 执行实际交易的actor
 * Created by dada on 2017/8/20.
 */
public class HuobiTrader extends AbstractActor {

    private static Logger logger = LoggerFactory.getLogger(HuobiTrader.class);

    public static Props props() {
        return Props.create(HuobiTrader.class, () -> new HuobiTrader());
    }

    private HuobiTradeClient huobiTradeClient;

    private AtomicInteger tradeCount = new AtomicInteger(0);

    public HuobiTrader() {
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(TradeInfo.class, trade -> {
            if(huobiTradeClient == null) {
                huobiTradeClient = ApplicationContextHolder.getBean(HuobiTradeClient.class);
            }

            if(huobiTradeClient == null) {
                logger.error("ERROR on init huobiTradeClient");
                return;
            }
            try{
                logger.info("Receive huobi trade {}", trade);
                if(!CoinTradeContext.DO_TRADE) {
                    return;
                }
                if( CoinTradeContext.MAX_TRADE_COUNT > 0 && tradeCount.getAndIncrement() >= CoinTradeContext.MAX_TRADE_COUNT) {
                    logger.info("Reach max huobi trade count");
                    return;
                }
                String orderId = huobiTradeClient.createEthOrder(trade.getAmount(), trade.getPrice(), trade.getAction());
                trade.setOrderId(orderId);
                logger.info("Order is is {} for {}", orderId, trade);
//                HuobiTradeMonitor.getTobeConfirmedTradeQueue().add(trade);
            } catch (ApiException e) {
                trade.setAction(TradeAction.FAIL);
                trade.setComment(e.getMessage());
                logger.error("ERROR on huobi coin trade ", e);
                System.exit(1);
//                HuobiTradeMonitor.getTobeConfirmedTradeQueue().add(trade);
            }
            catch (Exception e) {
                logger.error("ERROR on handle {}", trade, e);
            }

        }).build();
    }
}
