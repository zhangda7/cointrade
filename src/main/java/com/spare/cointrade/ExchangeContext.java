package com.spare.cointrade;

import com.alibaba.fastjson.JSON;
import com.spare.cointrade.model.CoinType;
import com.spare.cointrade.model.OpenExchangeData;
import com.spare.cointrade.model.TradePlatform;
import com.spare.cointrade.util.FileUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;

/**
 * fetch exchange from /home/admin/data/exchange.json
 * or update from https://openexchangerates.org/api/latest.json?app_id=${}
 */
public class ExchangeContext {

//    public static CoinType[] toCheckedCoin = new CoinType[] {CoinType.BTC, CoinType.QTUM, CoinType.ETH};

    public static CoinType[] toCheckedCoin = new CoinType[] {CoinType.BTC, CoinType.ETH, CoinType.LTC, CoinType.QTUM, CoinType.EOS, CoinType.BTG};

    private static final String OPEN_EXCHANGE_FILE = "/home/admin/data/exchange.txt";

    static {
        String openExchange = FileUtil.readFileByLinesToOneString(OPEN_EXCHANGE_FILE);
        openExchangeData = JSON.parseObject(openExchange, OpenExchangeData.class);
    }

    /**
     * 存储各个币种对USDT的转换数目
     */
    public static Map<CoinType, Double> binanceCoinUsdtMap = new HashMap<>();

    private static OpenExchangeData openExchangeData;

    private static Double KRW2USD() {
        return 1 / openExchangeData.getRates().get("KRW");
    }

    /**
     * 设定的归一化的币种
     * 目前设定为CNY
     * @return
     */
    public static CoinType normalizeCoinType() {
        return CoinType.CNY;
    }

    public static Double normalizeToUSD(CoinType coinType, Double oriPrice) {
        switch (coinType) {
            case KRW:
                return oriPrice * KRW2USD();
            case USDT:
                return oriPrice;
            default:
                return null;
        }
    }

    /**
     * 获取各个平台的一个币种对USDT的交易汇率
     * @return
     */
    public static Double currency2USDT(TradePlatform tradePlatform, CoinType coinType) {
        if(! tradePlatform.equals(TradePlatform.BINANCE)) {
            throw new IllegalArgumentException("Only binance support convert2USDT function");
        }
        if(! binanceCoinUsdtMap.containsKey(coinType)) {
            throw new IllegalArgumentException("Can not convert " + coinType + " to USDT");
        }
        return binanceCoinUsdtMap.get(coinType);
    }

}
