package com.spare.cointrade;

import akka.actor.ActorSystem;
import com.spare.cointrade.account.AccountManager;
import com.spare.cointrade.actor.consumer.HuobiConsumer;
import com.spare.cointrade.actor.monitor.ListingInfoMonitor;
import com.spare.cointrade.actor.monitor.TradeStateSyncer;
import com.spare.cointrade.actor.trade.HuobiTrader;
import com.spare.cointrade.actor.trade.OkCoinTrader;
import com.spare.cointrade.actor.trade.TradeJudgeV2;
import com.spare.cointrade.model.Account;
import com.spare.cointrade.util.AkkaContext;
import com.spare.cointrade.util.ApplicationContextHolder;
import com.spare.cointrade.util.CoinTradeConstants;
import com.spare.cointrade.util.SpringExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;

/**
 * 回放数据时使用
 * Created by dada on 2017/7/16.
 */
@SpringBootApplication
public class ReplayCoinApplicationMain {

    @Autowired
    private ApplicationContext applicationContext;

    public static void initActor() {
        AkkaContext.getSystem().actorOf(TradeJudgeV2.props(), "tradeJudge");
        AkkaContext.getSystem().actorOf(HuobiTrader.props(), "huobiTrader");
        AkkaContext.getSystem().actorOf(HuobiConsumer.props(), "huobiConsumer");
        AkkaContext.getSystem().actorOf(OkCoinTrader.props(), "okCoinTrader");
        AkkaContext.getSystem().actorOf(ListingInfoMonitor.props(), CoinTradeConstants.ACTOR_LISTING_INFO_MONITOR);
        AkkaContext.getSystem().actorOf(TradeStateSyncer.props(), CoinTradeConstants.ACTOR_TRADE_STATE_SYNCER);
    }

    private static void mockAccount() {
        Account bithumb = AccountManager.INSTANCE.mockBithumbAccount();
        Account binance = AccountManager.INSTANCE.mockBinancebAccount();
        AccountManager.INSTANCE.addAccount(binance);
        AccountManager.INSTANCE.addAccount(bithumb);
    }

    public static void main(String[] args) {
        mockAccount();
        SpringApplication.run(ReplayCoinApplicationMain.class, args);
    }

//    @Bean
    public ActorSystem actorSystem() {
        ActorSystem system = AkkaContext.getSystem();
        SpringExtension.SPRING_EXTENSION_PROVIDER.get(system)
                .initialize(applicationContext);
        return system;
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
            String[] beanNames = ctx.getBeanDefinitionNames();
            Arrays.sort(beanNames);
            for (String beanName : beanNames) {
                System.out.println(beanName);
                ApplicationContextHolder.putBean(beanName.toUpperCase(), ctx.getBean(beanName));
            }
            initActor();
        };
    }

}