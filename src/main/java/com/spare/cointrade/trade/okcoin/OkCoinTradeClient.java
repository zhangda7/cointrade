package com.spare.cointrade.trade.okcoin;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.spare.cointrade.actor.trade.TradeJudge;
import com.spare.cointrade.model.AccountInfo;
import com.spare.cointrade.model.HuobiAccount;
import com.spare.cointrade.model.HuobiSubAccount;
import com.spare.cointrade.model.TradeAction;
import com.spare.cointrade.model.okcoin.OkCoinAccountFree;
import com.spare.cointrade.trade.okcoin.stock.IStockRestApi;
import com.spare.cointrade.trade.okcoin.stock.impl.StockRestApi;
import com.spare.cointrade.util.CoinTradeConstants;
import org.apache.http.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

/**
 * Created by dada on 2017/8/26.
 */
@Component
public class OkCoinTradeClient {

    private static Logger logger = LoggerFactory.getLogger(OkCoinTradeClient.class);

//    String api_key = "13633211-a872-44df-b0e8-8f63ed0ec3f7";  //OKCoin申请的apiKey
//    String secret_key = "D70A2FFECEE528EBC4DC49857C546A8D";  //OKCoin 申请的secret_key

    @Value("${okcoin.api.key}")
    private String api_key;

    @Value("${okcoin.api.secret}")
    private String secret_key;

    public OkCoinTradeClient(String api_key, String secret_key) {
        this.api_key = api_key;
        this.secret_key = secret_key;
    }

    public OkCoinTradeClient() {}

    String url_prex = "https://www.okcoin.cn";  //注意：请求URL 国际站https://www.okcoin.com ; 国内站https://www.okcoin.cn

    /**
     * get请求无需发送身份认证,通常用于获取行情，市场深度等公共信息
     *
     */
    IStockRestApi stockGet = new StockRestApi(url_prex);

    /**
     * post请求需发送身份认证，获取用户个人相关信息时，需要指定api_key,与secret_key并与参数进行签名，
     * 此处对构造方法传入api_key与secret_key,在请求用户相关方法时则无需再传入，
     * 发送post请求之前，程序会做自动加密，生成签名。
     *
     */
    IStockRestApi stockPost;
//    IStockRestApi stockPost = new StockRestApi(url_prex, api_key, secret_key);

    @PostConstruct
    public void init() {
        this.stockPost = new StockRestApi(url_prex, api_key, secret_key);
        try {
            fillAccountInfo();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void fillAccountInfo() throws IOException, HttpException {
        AccountInfo accountInfo = new AccountInfo();
        accountInfo.setSource(CoinTradeConstants.SOURCE_OKCOIN);
        accountInfo.setSymbol(CoinTradeConstants.SYMBOL_ETH);
        //{"result":true,"info":{"funds":{"borrow":{"btc":"0","bcc":"0","etc":"0","eth":"0","ltc":"0","cny":"0"},"asset":{"total":"1211.6306052","net":"1211.6306052"},"free":{"btc":"0.00748","bcc":"0","etc":"0","eth":"0.00998","ltc":"0","cny":"968.1554"},"freezed":{"btc":"0","bcc":"0","etc":"0","eth":"0","ltc":"0","cny":"0"}}}}
        String user = this.queryUser();
        JSONObject jsonObject = JSON.parseObject(user);
        OkCoinAccountFree okCoinAccountFree = JSON.parseObject(jsonObject.getJSONObject("info").getJSONObject("funds").getString("free"), OkCoinAccountFree.class);
        accountInfo.setCoinAmount(okCoinAccountFree.getEth());
        accountInfo.setMoney(okCoinAccountFree.getCny());

        TradeJudge.curStatus.setOkCoinAccount(accountInfo);
    }

    /**
     * api/v1/trade.do
     * @param price
     * @param amount
     * @return orderId
     * @throws IOException
     * @throws HttpException
     */
    public String createEthOrder(Double price, Double amount, TradeAction action) throws IOException, HttpException {
        //现货下单交易
        String tradeAction = "";
        if(action.equals(TradeAction.BUY)) {
            tradeAction = "buy";
        } else if(action.equals(TradeAction.SELL)) {
            tradeAction = "sell";
        }
        String tradeResult = stockPost.trade("eth_cny", tradeAction, String.valueOf(price), String.valueOf(amount));
        OkCoinResponse okCoinResponse = JSON.parseObject(tradeResult, OkCoinResponse.class);
        if(okCoinResponse.getResult() == false) {
            throw new IllegalArgumentException("ERROR happened on create eth order, error code " + okCoinResponse.getError_code() + ":" + JSON.toJSONString(okCoinResponse));
        }
        JSONObject tradeJSV1 = JSONObject.parseObject(tradeResult);
        String tradeOrderV1 = tradeJSV1.getString("order_id");
        return tradeOrderV1;
    }

    public OrderDetail queryOrder(String orderId) throws IOException, HttpException {
        String orderInfos = stockPost.order_info("eth_cny", orderId);
        OkCoinResponse okCoinResponse = JSON.parseObject(orderInfos, OkCoinResponse.class);
        if(okCoinResponse.getResult() == false) {
            throw new IllegalArgumentException("ERROR happened on create eth order, error code " + okCoinResponse.getError_code());
        }
        ;
        JSONObject orderInfosJson = JSONObject.parseObject(orderInfos);
        List<OrderDetail> orderDetailList = JSON.parseObject(orderInfosJson.getString("orders"), new TypeReference<List<OrderDetail>>() {});
        if(CollectionUtils.isEmpty(orderDetailList)) {
            return null;
        }
        return orderDetailList.get(0);
    }

    public String queryUser() throws IOException, HttpException {
        return stockPost.userinfo();
    }


}
