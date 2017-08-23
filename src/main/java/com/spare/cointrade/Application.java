package com.spare.cointrade;

import com.spare.cointrade.actor.trade.HuobiTrader;
import com.spare.cointrade.actor.trade.OkCoinTrader;
import com.spare.cointrade.actor.trade.TradeJudge;
import com.spare.cointrade.model.trade.HuobiTrade;
import com.spare.cointrade.util.AkkaContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Created by dada on 2017/7/16.
 */
@SpringBootApplication
public class Application {

    private static void initActor() {
        AkkaContext.getSystem().actorOf(TradeJudge.props(), "tradeJudge");
        AkkaContext.getSystem().actorOf(HuobiTrader.props(), "huobiTrader");
        AkkaContext.getSystem().actorOf(OkCoinTrader.props(), "okCoinTrader");
        AkkaContext.getSystem().actorOf(OkCoinTrader.props(), "huobiConsumer");
    }

    public static void main(String[] args) {
        initActor();
        SpringApplication.run(Application.class, args);
    }

}
