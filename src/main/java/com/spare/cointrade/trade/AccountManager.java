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
        account.getBalanceMap().put(CoinType.ETH, new Balance(CoinType.ETH, 1.0, 0.0));
        account.getBalanceMap().put(CoinType.LTC, new Balance(CoinType.LTC, 1.0, 0.0));
        account.getBalanceMap().put(CoinType.QTUM, new Balance(CoinType.QTUM, 1.0, 0.0));
        account.getBalanceMap().put(CoinType.KRW, new Balance(CoinType.KRW, 50000.0, 0.0));
        return account;
    }

    public Account mockBinancebAccount() {
        Account account = new Account();
        account.setTradePlatform(TradePlatform.BINANCE);
        account.setBalanceMap(new HashMap<>());
        account.getBalanceMap().put(CoinType.BTC, new Balance(CoinType.BTC, 1.0, 0.0));
        account.getBalanceMap().put(CoinType.ETH, new Balance(CoinType.ETH, 1.0, 0.0));
        account.getBalanceMap().put(CoinType.LTC, new Balance(CoinType.LTC, 1.0, 0.0));
        account.getBalanceMap().put(CoinType.QTUM, new Balance(CoinType.QTUM, 1.0, 0.0));
        account.getBalanceMap().put(CoinType.CNY, new Balance(CoinType.CNY, 10000.0, 0.0));
        return account;
    }
}
