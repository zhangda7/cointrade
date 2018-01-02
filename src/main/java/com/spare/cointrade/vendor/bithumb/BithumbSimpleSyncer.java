package com.spare.cointrade.vendor.bithumb;

import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class BithumbSimpleSyncer implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(BithumbSimpleSyncer.class);

    private static ScheduledExecutorService scheduledExecutorService;

    private static AtomicBoolean started = new AtomicBoolean();

    @PostConstruct
    public void start() {
        if(! started.compareAndSet(false, true)) {
            logger.error("Syncer has started");
            return;
        }
        scheduledExecutorService = Executors.newScheduledThreadPool(1, new DefaultThreadFactory("BithumbSimpleSyncer"));
        scheduledExecutorService.scheduleWithFixedDelay(this, 5, 5, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        try {
            Api_Client api = new Api_Client("api connect key",
                    "api secret key");
            HashMap<String, String> rgParams = new HashMap<String, String>();

            String result = api.callApi("/public/ticker/BTG", rgParams);
            System.out.println(result);
        } catch (Exception e) {
            logger.error("ERROR ", e);
        } catch (Throwable e) {
            logger.error("Throw ERROR ", e);
            throw e;
        }
    }
}
