package com.spare.cointrade;

import akka.actor.ActorSystem;
import com.spare.cointrade.actor.consumer.HuobiConsumer;
import com.spare.cointrade.actor.monitor.ListingInfoMonitor;
import com.spare.cointrade.actor.trade.HuobiTrader;
import com.spare.cointrade.actor.trade.OkCoinTrader;
import com.spare.cointrade.actor.trade.TradeJudgeV2;
import com.spare.cointrade.model.Account;
import com.spare.cointrade.model.CoinType;
import com.spare.cointrade.model.TradeType;
import com.spare.cointrade.realtime.huobi.HuobiClient;
import com.spare.cointrade.realtime.okcoin.OkcoinClient;
import com.spare.cointrade.trade.AccountManager;
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
 * Created by dada on 2017/7/16.
 */
@SpringBootApplication
public class CoinApplicationMain {

    @Autowired
    private ApplicationContext applicationContext;

    public static void initActor() {
//        AkkaContext.getSystem().actorOf(TradeJudge.props(), "tradeJudge");
        AkkaContext.getSystem().actorOf(TradeJudgeV2.props(), "tradeJudge");
        AkkaContext.getSystem().actorOf(HuobiTrader.props(), "huobiTrader");
        AkkaContext.getSystem().actorOf(HuobiConsumer.props(), "huobiConsumer");
//        AkkaContext.getSystem().actorOf(HuobiTradeMonitor.props(), "huobiTradeMonitor");
        AkkaContext.getSystem().actorOf(OkCoinTrader.props(), "okCoinTrader");
        AkkaContext.getSystem().actorOf(ListingInfoMonitor.props(), CoinTradeConstants.ACTOR_LISTING_INFO_MONITOR);

    }

    private static void mockAccount() {
        Account bithumb = AccountManager.INSTANCE.mockBithumbAccount();
        Account binance = AccountManager.INSTANCE.mockBinancebAccount();
        AccountManager.INSTANCE.addAccount(binance);
        AccountManager.INSTANCE.addAccount(bithumb);
    }

    public static void main(String[] args) {
//        initActor();
        mockAccount();
        SpringApplication.run(CoinApplicationMain.class, args);
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

//            System.out.println("Let's inspect the beans provided by Spring Boot:");
            String[] beanNames = ctx.getBeanDefinitionNames();
            Arrays.sort(beanNames);
            for (String beanName : beanNames) {
                System.out.println(beanName);
                ApplicationContextHolder.putBean(beanName.toUpperCase(), ctx.getBean(beanName));
            }
            initActor();
//            ApplicationContextHolder.getBean(HuobiClient.class).startFetch("market.ethbtc.depth.step0", CoinType.BTC, CoinType.ETH);
//            ApplicationContextHolder.getBean(OkcoinClient.class).startFetch(TradeType.COIN_COIN);
        };
    }

}
