package com.spare.cointrade.service;

import com.spare.cointrade.ExchangeContext;
import com.spare.cointrade.model.CoinType;
import com.spare.cointrade.model.TradePlatform;

import java.util.HashMap;
import java.util.Map;

/**
 * 获取各个平台的手续费，手续费有几个收取方法
 * 1.基础的(扣除收取到的资产)，即收取到手的货币手续费，即买币时，实际收到的币是扣掉手续费后的，卖币时，收到的钱是扣掉手续费后的
 * 2.币安，如果有BNB，则统一以BNB计算手续费
 * 3.其他策略
 */
public class ServiceChargeContext {

    private static Map<TradePlatform, Double> simpleServiceChargeMap = new HashMap<>();

    private static Map<TradePlatform, Map<CoinType, Double>> serviceChargeMap = new HashMap<>();

    public static Double MAX_SERVICE_CHARGR = 0.0015;

    /**
     * BITHUMB未优惠的费率 0.15%
     */
    private static Double BITHUMB_BASIC_FEE = 0.0015;

    /**
     * 使用BNB交易的费率  0.05%
     */
    private static Double BINANCE_BASIC_FEE = 0.0005;

    static {
        serviceChargeMap.put(TradePlatform.BINANCE, new HashMap<>());
        serviceChargeMap.put(TradePlatform.BITHUMB, new HashMap<>());

        simpleServiceChargeMap.put(TradePlatform.BITHUMB, BITHUMB_BASIC_FEE);
        simpleServiceChargeMap.put(TradePlatform.BINANCE, BINANCE_BASIC_FEE);

        for (CoinType coinType : ExchangeContext.toCheckedCoin) {
            serviceChargeMap.get(TradePlatform.BITHUMB).put(coinType, BITHUMB_BASIC_FEE);
        }

        for (CoinType coinType : ExchangeContext.toCheckedCoin) {
            serviceChargeMap.get(TradePlatform.BINANCE).put(coinType, BINANCE_BASIC_FEE);
        }
    }

    public static Double getSimpleServiceCharge(TradePlatform platform) {
        return simpleServiceChargeMap.get(platform);
    }

}
