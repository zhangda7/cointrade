package com.spare.cointrade.trade;

import com.spare.cointrade.model.Account;
import com.spare.cointrade.model.Balance;
import com.spare.cointrade.model.CoinType;
import com.spare.cointrade.model.TradePlatform;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AccountManager {

    public static AccountManager INSTANCE = new AccountManager();

    private Map<TradePlatform, Account> platformAccountMap;

    private AccountManager() {
        platformAccountMap = new ConcurrentHashMap<>();
    }

    public Map<TradePlatform, Account> getPlatformAccountMap() {
        return platformAccountMap;
    }

    public void addAccount(Account account) {
        platformAccountMap.put(account.getTradePlatform(), account);
    }

//    /**
//     * 统一化各个平台的现金获取
//     * 全部转换为CNY返回
//     * @param tradePlatform
//     * @return
//     */
//    public double getNormalizeCNY(TradePlatform tradePlatform) {
//        switch (tradePlatform) {
//            case BITHUMB:
//                return platformAccountMap.get(TradePlatform.BITHUMB).getBalanceMap().get(CoinType.KRW).getFreeAmount() * ExchangeContext.KRW2CNY();
//            case BINANCE:
//                return platformAccountMap.get(TradePlatform.BINANCE).getBalanceMap().get(CoinType.CNY).getFreeAmount();
//            default:
//                return 0.0;
//        }
//    }

    public Double getFreeAmount(TradePlatform tradePlatform, CoinType coinType) {
        return platformAccountMap.get(tradePlatform).getBalanceMap().get(coinType).getFreeAmount();
    }

    public void increaseAmount(TradePlatform tradePlatform, CoinType coinType, Double amount) {
        if(! platformAccountMap.containsKey(tradePlatform)) {
            throw new IllegalArgumentException("Illegal platform "+ tradePlatform);
        }
        if(! platformAccountMap.get(tradePlatform).getBalanceMap().containsKey(coinType)) {
            throw new IllegalArgumentException("Illegal platform "+ tradePlatform + ", cointype " + coinType);
        }
        platformAccountMap.get(tradePlatform).getBalanceMap().get(coinType).setFreeAmount(
                platformAccountMap.get(tradePlatform).getBalanceMap().get(coinType).getFreeAmount() + amount);
    }

    public void decreaseAmount(TradePlatform tradePlatform, CoinType coinType, Double amount) {
        if(! platformAccountMap.containsKey(tradePlatform)) {
            throw new IllegalArgumentException("Illegal platform "+ tradePlatform);
        }
        if(! platformAccountMap.get(tradePlatform).getBalanceMap().containsKey(coinType)) {
            throw new IllegalArgumentException("Illegal platform "+ tradePlatform + ", cointype " + coinType);
        }
        platformAccountMap.get(tradePlatform).getBalanceMap().get(coinType).setFreeAmount(
                platformAccountMap.get(tradePlatform).getBalanceMap().get(coinType).getFreeAmount() - amount);
    }

    public Account mockBithumbAccount() {
        Account account = new Account();
        account.setTradePlatform(TradePlatform.BITHUMB);
        account.setBalanceMap(new HashMap<>());
        account.getBalanceMap().put(CoinType.BTC, new Balance(CoinType.BTC, 1.0, 0.0));
        account.getBalanceMap().put(CoinType.ETH, new Balance(CoinType.ETH, 10.0, 0.0));
        account.getBalanceMap().put(CoinType.LTC, new Balance(CoinType.LTC, 100.0, 0.0));
        account.getBalanceMap().put(CoinType.QTUM, new Balance(CoinType.QTUM, 100.0, 0.0));
        account.getBalanceMap().put(CoinType.EOS, new Balance(CoinType.EOS, 100.0, 0.0));
        account.getBalanceMap().put(CoinType.BTG, new Balance(CoinType.BTG, 100.0, 0.0));
        account.getBalanceMap().put(CoinType.KRW, new Balance(CoinType.KRW, 200000.0, 0.0));
        return account;
    }

    public Account mockBinancebAccount() {
        Account account = new Account();
        account.setTradePlatform(TradePlatform.BINANCE);
        account.setBalanceMap(new HashMap<>());
        account.getBalanceMap().put(CoinType.BTC, new Balance(CoinType.BTC, 1.0, 0.0));
        account.getBalanceMap().put(CoinType.ETH, new Balance(CoinType.ETH, 10.0, 0.0));
        account.getBalanceMap().put(CoinType.LTC, new Balance(CoinType.LTC, 100.0, 0.0));
        account.getBalanceMap().put(CoinType.QTUM, new Balance(CoinType.QTUM, 100.0, 0.0));
        account.getBalanceMap().put(CoinType.EOS, new Balance(CoinType.EOS, 100.0, 0.0));
        account.getBalanceMap().put(CoinType.BTG, new Balance(CoinType.BTG, 100.0, 0.0));
         account.getBalanceMap().put(CoinType.USDT, new Balance(CoinType.USDT, 1000.0, 0.0));
        return account;
    }
}
