package com.spare.cointrade.util;

import com.spare.cointrade.model.TradePlatform;

import java.util.HashMap;
import java.util.Map;


public class TradingFeesUtil {

    /**
     * binance https://www.binance.com/fees.html
     * bithumb https://www.bithumb.com/u1/US138
     */

    private static Map<TradePlatform, Double> feesMap = new HashMap<>();

    public static boolean USE_BNB = false;

    static {
        feesMap.put(TradePlatform.BITHUMB, 0.0015);

        if(USE_BNB) {
            feesMap.put(TradePlatform.BINANCE, 0.0);
        } else {
            feesMap.put(TradePlatform.BINANCE, 0.001);
        }
    }

    public static Double getTradeFee(TradePlatform tradePlatform) {
        return feesMap.get(tradePlatform);
    }
}
