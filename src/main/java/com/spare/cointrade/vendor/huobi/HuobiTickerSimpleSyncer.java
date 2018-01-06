package com.spare.cointrade.vendor.huobi;

import com.spare.cointrade.trade.huobi.ApiClient;
import com.spare.cointrade.trade.huobi.Symbol;
import com.spare.cointrade.vendor.bithumb.Api_Client;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class HuobiTickerSimpleSyncer implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(HuobiTickerSimpleSyncer.class);

    private static ScheduledExecutorService scheduledExecutorService;

    private static AtomicBoolean started = new AtomicBoolean();

//    @PostConstruct
    public void start() {
        if(! started.compareAndSet(false, true)) {
            logger.error("Syncer has started");
            return;
        }
        scheduledExecutorService = Executors.newScheduledThreadPool(1, new DefaultThreadFactory("BithumbSimpleSyncer"));
        scheduledExecutorService.scheduleWithFixedDelay(this, 5, 2, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        try {
            ApiClient apiClient = new ApiClient("1", "2");
            List<Symbol> symbolList = apiClient.getSymbols();
            System.out.println(symbolList);
        } catch (Exception e) {
            logger.error("ERROR ", e);
        } catch (Throwable e) {
            logger.error("Throw ERROR ", e);
            throw e;
        }
    }

    public static void main(String[] args) {
        HuobiTickerSimpleSyncer simpleSyncer = new HuobiTickerSimpleSyncer();
        simpleSyncer.run();
    }
}
