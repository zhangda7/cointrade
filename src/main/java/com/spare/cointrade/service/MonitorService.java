package com.spare.cointrade.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.spare.cointrade.model.AccountInfo;
import com.spare.cointrade.model.HuobiAccount;
import com.spare.cointrade.model.HuobiSubAccount;
import com.spare.cointrade.model.okcoin.OkCoinAccountFree;
import com.spare.cointrade.trade.huobi.HuobiTradeClient;
import com.spare.cointrade.trade.okcoin.OkCoinTradeClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by dada on 2017/9/9.
 */
@Component
public class MonitorService implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(MonitorService.class);

    @Autowired
    HuobiTradeClient huobiTradeClient;

    @Autowired
    OkCoinTradeClient okCoinTradeClient;

    private static ScheduledExecutorService scheduledExecutorService;

    private double totalMoney = 0.0;

    private double totalCoin = 0.0;

    private final int GAP = 20 * 1000;

    private long lastTs = 0;

    @PostConstruct
    private void start() {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleWithFixedDelay(this,5, 5, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        try {
            String user = okCoinTradeClient.queryUser();
            JSONObject jsonObject = JSON.parseObject(user);
            OkCoinAccountFree okCoinAccountFree = JSON.parseObject(jsonObject.getJSONObject("info").getJSONObject("funds").getString("free"), OkCoinAccountFree.class);
//            accountInfo.setCoinAmount(okCoinAccountFree.getEth());
//            accountInfo.setMoney(okCoinAccountFree.getCny());

            AccountInfo huobiAccountInfo = new AccountInfo();
            HuobiAccount huobiAccount = huobiTradeClient.queryBalance();
            for (HuobiSubAccount huobiSubAccount : huobiAccount.getList()) {
                if(huobiSubAccount.getCurrency().equals("cny") && huobiSubAccount.getType().equals("trade")) {
                    huobiAccountInfo.setMoney(Double.valueOf(huobiSubAccount.getBalance()));
                }
                if(huobiSubAccount.getCurrency().equals("eth") && huobiSubAccount.getType().equals("trade")) {
                    huobiAccountInfo.setCoinAmount(Double.valueOf(huobiSubAccount.getBalance()));
                }
            }

            boolean print = false;

            if(totalMoney != okCoinAccountFree.getCny() + huobiAccountInfo.getMoney()) {
                totalMoney = okCoinAccountFree.getCny() + huobiAccountInfo.getMoney();
                print = true;
            }
            if(totalCoin != okCoinAccountFree.getEth() + huobiAccountInfo.getCoinAmount()) {
                totalCoin = okCoinAccountFree.getEth() + huobiAccountInfo.getCoinAmount();
                print = true;
            }

            if(System.currentTimeMillis() - lastTs > GAP) {
                print = true;
                lastTs = System.currentTimeMillis();
            }

            if(print) {
                logger.info("[ okcoin {} {} ] [ huobi {} {} ] [ total {} {}]",
                        okCoinAccountFree.getCny(), okCoinAccountFree.getEth(),
                        huobiAccountInfo.getMoney(), huobiAccountInfo.getCoinAmount(),
                        okCoinAccountFree.getCny() + huobiAccountInfo.getMoney(),
                        okCoinAccountFree.getEth() + huobiAccountInfo.getCoinAmount());
            }

        } catch (Exception e) {
            logger.error("ERROR ", e);
        } catch (Throwable e) {
            logger.error("THROW ERROR ", e);
        }

    }
}
