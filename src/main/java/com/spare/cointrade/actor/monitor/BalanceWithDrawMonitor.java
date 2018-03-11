package com.spare.cointrade.actor.monitor;

import com.alibaba.fastjson.JSON;
import com.spare.cointrade.CoinApplicationMain;
import com.spare.cointrade.account.AccountManager;
import com.spare.cointrade.model.*;
import com.spare.cointrade.service.TradeHistoryService;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 使用提现平衡两边币值的monitor
 * 判断2边现有的币的数目，如果出现了币的数量完全偏到一边的情况，则进行2边的提现
 */
//@Component
public class BalanceWithDrawMonitor implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(BalanceWithDrawMonitor.class);

    private static AtomicBoolean started = new AtomicBoolean(false);

    private static ScheduledExecutorService scheduledExecutorService;

//    @PostConstruct
    private void start() {
        if(! started.compareAndSet(false, true)) {
            logger.error("BalanceWithDrawMonitor has started, just return");
            return;
        }
        if(! CoinApplicationMain.enableBalanceWithDraw) {
            logger.warn("Not enable balance with draw, just return");
            return;
        }
        scheduledExecutorService = Executors.newScheduledThreadPool(1, new DefaultThreadFactory("BalanceWithDrawMonitor"));
        scheduledExecutorService.scheduleWithFixedDelay(this, 10 * 60, 10 * 60, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        try {
            //TODO mock的方式，使用定时检查的方式，后续必须阈值的方式不一定满足的。因为不一定要所有的币都搬过去，才需要提币
            Account sourceAccount = AccountManager.INSTANCE.getPlatformAccountMap().get(TradePlatform.BINANCE);
            Account targetAccount = AccountManager.INSTANCE.getPlatformAccountMap().get(TradePlatform.BITHUMB);

            for(Map.Entry<CoinType, Balance> entry : sourceAccount.getBalanceMap().entrySet()) {
                Balance anotherBalance = targetAccount.getBalanceMap().get(entry.getKey());
                if(anotherBalance == null) {
                    continue;
                }
                Double delta = Math.abs(entry.getValue().getFreeAmount() - anotherBalance.getFreeAmount());
                Double sum = entry.getValue().getFreeAmount() + anotherBalance.getFreeAmount();
                if(delta / sum > 0.9) {
                    //暂且设定这个阈值，不用等完全搬过去
                    //need to with draw
                    Double transfer = delta / 2;
                    if(entry.getValue().getFreeAmount() > anotherBalance.getFreeAmount()) {
                        //from source account -> target account
                        mockWithDraw(sourceAccount, targetAccount, entry.getKey(), transfer);
                    } else {
                        //from target account -> source account
                        mockWithDraw(targetAccount, sourceAccount, entry.getKey(), transfer);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("ERROR ", e);
        } catch (Throwable e) {
            logger.error("THROW ERROR ", e);
        }
    }

    /**
     * 从source platform 提币到targetPlatform
     * @param sourceAccount
     * @param targetAccount
     * @param coinType
     * @param amount 要提币的数量
     */
    private void mockWithDraw(Account sourceAccount, Account targetAccount, CoinType coinType, double amount) {
        TradeHistory tradeHistory = new TradeHistory();
        tradeHistory.setPairId("WITH_DRAW");
        tradeHistory.setDirection(TradeDirection.FORWARD.name());
        tradeHistory.setTradePlatform(sourceAccount.getTradePlatform());
        tradeHistory.setTradeAction(TradeAction.WITH_DRAW);
        tradeHistory.setCoinType(coinType);
        tradeHistory.setTargetCoinType(coinType);
        tradeHistory.setPrice(0.0);
        tradeHistory.setAmount(amount);
        tradeHistory.setResult(TradeResult.PROCESSING);
        tradeHistory.setProfit(0.0);
        tradeHistory.setNormalizePrice(0.0);
        Balance sourceBalance = sourceAccount.getBalanceMap().get(coinType);
        Balance targetBalance = targetAccount.getBalanceMap().get(coinType);

        tradeHistory.setPreAccountSourceAmount(sourceBalance.getFreeAmount());
        tradeHistory.setPreAccountTargetAmount(sourceBalance.getFreeAmount() - amount);

        tradeHistory.setAfterAccountSourceAmount(targetBalance.getFreeAmount());
        tradeHistory.setAfterAccountTargetAmount(targetBalance.getFreeAmount() + amount);

        //这2行模拟真实的提币过程
        sourceBalance.setFreeAmount(sourceBalance.getFreeAmount() - amount);
        targetBalance.setFreeAmount(targetBalance.getFreeAmount() + amount);

        tradeHistory.setAccountName(sourceAccount.getAccountName());
        logger.info("Prepare insert withdraw tradeHistory {}", JSON.toJSONString(tradeHistory));
        TradeHistoryService.INSTANCE.insert(tradeHistory);
    }
}
