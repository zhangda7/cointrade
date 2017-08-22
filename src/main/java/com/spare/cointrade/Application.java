package com.spare.cointrade;

import com.spare.cointrade.actor.trade.TradeJudge;
import com.spare.cointrade.util.AkkaContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Created by dada on 2017/7/16.
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {

        AkkaContext.getSystem().actorOf(TradeJudge.props(), "tradeJudge");
        SpringApplication.run(Application.class, args);
    }

}
