package com.spare.cointrade.trade.huobi;

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

    @PostConstruct
    private void init() throws IllegalAccessException {
        apiClient = new ApiClient(API_KEY, API_SECRET);
        List<Account> accounts = apiClient.getAccounts();
        if(CollectionUtils.isEmpty(accounts)) {
            throw new IllegalAccessException("Huobi account size == 0");
        }
        if(accounts.size() > 1) {
            throw new IllegalAccessException("Huobi account size is > 1 : " + accounts.size());
        }
        this.accountId = String.valueOf(accounts.get(0).id);
    }

    public Long createEtcOrder(Double amount, Double price) {
        CreateOrderRequest createOrderReq = new CreateOrderRequest();
        createOrderReq.accountId = String.valueOf(accountId);
        createOrderReq.amount = String.valueOf(amount);
        createOrderReq.price = String.valueOf(price);
        createOrderReq.symbol = "etccny";
//        createOrderReq.symbol = "ethcny";
        createOrderReq.type = CreateOrderRequest.OrderType.BUY_LIMIT;
        Long orderId = apiClient.createOrder(createOrderReq);
        //TODO 确认place的作用
        String r = apiClient.placeOrder(orderId);
        print(r);
        return orderId;
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
