package com.spare.cointrade.trade.huobi;

import com.spare.cointrade.actor.trade.TradeJudge;
import com.spare.cointrade.model.AccountInfo;
import com.spare.cointrade.model.HuobiAccount;
import com.spare.cointrade.model.HuobiSubAccount;
import com.spare.cointrade.model.TradeAction;
import com.spare.cointrade.util.CoinTradeConstants;
import com.spare.cointrade.util.CoinTradeContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;

/**
 * Created by dada on 2017/8/22.
 */
@Component
public class HuobiTradeClient {

//    static final String API_KEY = "5e433d0a-a42aa575-afb58664-02d56";
//    static final String API_SECRET = "2b0b8da3-8a5e9845-f1707b00-9f1e0";

    @Value("${HUOBI_AK}")
    private String API_KEY;

    @Value("${HUOBI_AS}")
    private String API_SECRET;

    private ApiClient apiClient;

    private String accountId;

    public HuobiTradeClient() {

    }

    public HuobiTradeClient(String API_KEY, String API_SECRET) {
        this.API_KEY = API_KEY;
        this.API_SECRET = API_SECRET;
    }

    @PostConstruct
    public void init() throws IllegalAccessException {
        apiClient = new ApiClient(API_KEY, API_SECRET);
        List<Account> accounts = apiClient.getAccounts();
        if(CollectionUtils.isEmpty(accounts)) {
            throw new IllegalAccessException("Huobi account size == 0");
        }
        if(accounts.size() > 1) {
            throw new IllegalAccessException("Huobi account size is > 1 : " + accounts.size());
        }
        this.accountId = String.valueOf(accounts.get(0).id);

        fillAccountInfo();
    }

    private void fillAccountInfo() {
        AccountInfo accountInfo = new AccountInfo();
        accountInfo.setSource(CoinTradeConstants.SOURCE_HUOBI);
        accountInfo.setSymbol(CoinTradeConstants.SYMBOL_ETH);
        HuobiAccount huobiAccount = this.queryBalance();
        accountInfo.setType(huobiAccount.getType());
        accountInfo.setState(huobiAccount.getState());
        accountInfo.setId(String.valueOf(huobiAccount.getId()));

        for (HuobiSubAccount huobiSubAccount : huobiAccount.getList()) {
            if(huobiSubAccount.getCurrency().equals("cny") && huobiSubAccount.getType().equals("trade")) {
                accountInfo.setMoney(Double.valueOf(huobiSubAccount.getBalance()));
            }
            if(huobiSubAccount.getCurrency().equals("eth") && huobiSubAccount.getType().equals("trade")) {
                accountInfo.setCoinAmount(Double.valueOf(huobiSubAccount.getBalance()));
            }
        }
        TradeJudge.curStatus.setHuobiAccount(accountInfo);
    }

//    public String createEtcOrder(Double amount, Double price, TradeAction action) {
//        CreateOrderRequest createOrderReq = new CreateOrderRequest();
//        createOrderReq.accountId = String.valueOf(accountId);
//        createOrderReq.amount = String.valueOf(amount);
//        createOrderReq.price = String.valueOf(price);
//        createOrderReq.symbol = "etccny";
//        if(action.equals(TradeAction.BUY)) {
//            createOrderReq.type = CreateOrderRequest.OrderType.BUY_LIMIT;
//        } else if(action.equals(TradeAction.SELL)) {
//            createOrderReq.type = CreateOrderRequest.OrderType.SELL_LIMIT;
//        }
//        Long orderId = apiClient.createOrder(createOrderReq);
//        //TODO 确认place的作用
//        String r = apiClient.placeOrder(orderId);
//        print(r);
//        return r;
//    }

    public String createEthOrder(Double amount, Double price, TradeAction action) {
        CreateOrderRequest createOrderReq = new CreateOrderRequest();
        createOrderReq.accountId = String.valueOf(accountId);
        createOrderReq.amount = String.valueOf(amount);
        createOrderReq.price = String.valueOf(price);
        createOrderReq.symbol = "ethcny";
        if(action.equals(TradeAction.BUY)) {
            createOrderReq.type = CreateOrderRequest.OrderType.BUY_LIMIT;
        } else if(action.equals(TradeAction.SELL)) {
            createOrderReq.type = CreateOrderRequest.OrderType.SELL_LIMIT;
        }
        Long orderId = apiClient.createOrder(createOrderReq);
        String r = apiClient.placeOrder(orderId);
        print(r);
        //r is the string orderId
        return r;
    }

    public OrderDetail queryOrder(String orderId) {
        return apiClient.queryOrder(orderId);
    }

    public HuobiAccount queryBalance() {
        return apiClient.queryBalance(this.accountId);
    }

    public String cancelOrder(String orderId) {
        return apiClient.cancelOrder(orderId);
    }

    private void curAccountStatus() {
    }

    public void apiSample() {
        // create ApiClient using your api key and api secret:
        ApiClient client = new ApiClient(API_KEY, API_SECRET);
        // get symbol list:
        print(client.getSymbols());
        // get accounts:
        List<Account> accounts = client.getAccounts();
        print(accounts);
        if (!accounts.isEmpty()) {
            // find account id:
            Account account = accounts.get(0);
            long accountId = account.id;
            // create order:
            CreateOrderRequest createOrderReq = new CreateOrderRequest();
            createOrderReq.accountId = String.valueOf(accountId);
            createOrderReq.amount = "0.02";
            createOrderReq.price = "1100.99";
            createOrderReq.symbol = "ethcny";
            createOrderReq.type = CreateOrderRequest.OrderType.BUY_LIMIT;
            Long orderId = client.createOrder(createOrderReq);
            print(orderId);
            // place order:
            String r = client.placeOrder(orderId);
            print(r);
        }
    }

    static void print(Object obj) {
        try {
            System.out.println(JsonUtil.writeValue(obj));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
