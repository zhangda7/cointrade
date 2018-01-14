package com.spare.cointrade;

import com.spare.cointrade.model.CoinType;

public class ExchangeContext {

    public static CoinType[] toCheckedCoin = new CoinType[] {CoinType.BTC, CoinType.QTUM, CoinType.ETH};

//    public static CoinType[] toCheckedCoin = new CoinType[] {CoinType.BTC, CoinType.ETH, CoinType.LTC, CoinType.QTUM, CoinType.EOS, CoinType.BTG};

    public static double KRW2CNY() {
        return 0.006107;
    }

    public static double USD2CNY() {
        return 6.4915;
    }

    /**
     * 设定的归一化的币种
     * 目前设定为CNY
     * @return
     */
    public static CoinType normalizeCoinType() {
        return CoinType.CNY;
    }

}
