package com.spare.cointrade.util;

import java.util.Comparator;

/**
 * Created by dada on 2017/8/30.
 */
public class CoinTradeConstants {

    public static final String ACTOR_TRADE_JUDGE = "tradeJudge";

    public static final String ACTOR_LISTING_INFO_MONITOR = "listingInfoMonitor";

    public static final String ACTOR_DEPTH_INFO_HISTORY_MONITOR = "depthInfoHistoryMonitor";

    public static final String ACTOR_TRADE_STATE_SYNCER = "tradeStateSyncer";

    public static final String SOURCE_HUOBI = "HUOBI";

    public static final String SOURCE_OKCOIN = "okcoin";


    public static final String SYMBOL_ETH = "ETH";

    public static final Double FIX_SERVICE_CHARGE = 0.0021;

    public static final Comparator<Double> COMPARATOR_BUY_DEPTH = new Comparator<Double>() {
        @Override
        public int compare(Double o1, Double o2) {
            return o1.compareTo(o2);
        }
    };

    public static final Comparator<Double> COMPARATOR_SELL_DEPTH = new Comparator<Double>() {
        @Override
        public int compare(Double o1, Double o2) {
            return o2.compareTo(o1);
        }
    };
}
