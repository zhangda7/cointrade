package com.spare.cointrade.controller;

import akka.actor.ActorRef;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.spare.cointrade.actor.monitor.ListingInfoMonitor;
import com.spare.cointrade.actor.trade.TradeJudge;
import com.spare.cointrade.actor.trade.TradeJudgeV2;
import com.spare.cointrade.model.*;
import com.spare.cointrade.service.TradeHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by dada on 2017/7/16.
 */
@CrossOrigin(origins = "*")
@RestController
public class StatusController {

    private static Logger logger = LoggerFactory.getLogger(StatusController.class);

    private static final int CODE_SUCCESS = 200;

    private static final int CODE_FAIL = 400;


    @RequestMapping("/tradestatus")
    public String tradeStatus() {
        return JSON.toJSONString(TradeJudge.curStatus);
    }

    @RequestMapping("/tradestatus2")
    public String tradeStatus2() {
        return JSON.toJSONString(TradeJudgeV2.curStatus);
    }


    @RequestMapping("/listingInfo")
    public String listingInfo() {
        return JSON.toJSONString(ListingInfoMonitor.listingFullInfoMap);
    }

    @RequestMapping("/listingOkexInfo")
    public String listingOkexInfo() {
        return listingBuyInfo(TradePlatform.OKEX, CoinType.BTC);
    }

    @RequestMapping("/listingHuobiInfo")
    public String listingHuobiInfo() {
        return listingBuyInfo(TradePlatform.HUOBI, CoinType.BTC);
    }

    @RequestMapping("/listingPriceInfo")
    public String listingPriceInfo(@RequestParam("platform") String platform, @RequestParam("sourcecoin") String sourcecoin) {
        return listingBuyInfo(TradePlatform.valueOf(platform), CoinType.valueOf(sourcecoin));
    }

    @RequestMapping("/listingDelta")
    public String listingDelta(@RequestParam("platform") String platform, @RequestParam("sourcecoin") String sourcecoin) {
        return listingBuyInfo(TradePlatform.valueOf(platform), CoinType.valueOf(sourcecoin));
    }

    @RequestMapping("/listingTradeHistory")
    public String listingTradeHistory() {
        RestfulPage restfulPage = new RestfulPage();
        restfulPage.setCode(CODE_SUCCESS);
        try {
            List<TradeHistory> tradeHistoryList = TradeHistoryService.INSTANCE.list();
            restfulPage.setCount(tradeHistoryList.size());
            restfulPage.setData(JSON.toJSONString(tradeHistoryList));
            return JSON.toJSONString(restfulPage);
        } catch (Exception e) {
            logger.error("ERROR ", e);
        }
        restfulPage.setCode(CODE_FAIL);
        return JSON.toJSONString(restfulPage);
    }

    public String listingBuyInfo(TradePlatform platform, CoinType sourceCoin) {
        RestfulPage restfulPage = new RestfulPage();
        restfulPage.setCode(CODE_SUCCESS);
        JSONObject retObject = new JSONObject();
        int maxRecords = 2;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        for (ListingFullInfo listingFullInfo : ListingInfoMonitor.listingFullInfoMap.values()) {
            if(! listingFullInfo.getTradePlatform().equals(platform)) {
                continue;
            }
            if(! listingFullInfo.getSourceCoinType().equals(sourceCoin)) {
                continue;
            }
//            String key = listingFullInfo.toKey();
            String key = "LISTING_PRICE";
            JSONArray jsonArray = new JSONArray();
            int i = 1;
            for(ListingDepth.DepthInfo depthInfo : listingFullInfo.getBuyDepth().getDepthInfoMap().values()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", "卖(" + i + ")");
                jsonObject.put("price", depthInfo.getNormalizePrice());
                jsonObject.put("amount", depthInfo.getAmount());
                jsonObject.put("type", "sell");
                //每次都在第0个位置插入，才能实现倒排
                jsonArray.add(0, jsonObject);
                if(i++ >= maxRecords) {
                    break;
                }
            }
            i = 1;
            for(ListingDepth.DepthInfo depthInfo : listingFullInfo.getSellDepth().getDepthInfoMap().values()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", "买(" + i + ")");
                jsonObject.put("price", depthInfo.getNormalizePrice());
                jsonObject.put("amount", depthInfo.getAmount());
                jsonObject.put("type", "buy");
                jsonArray.add(jsonObject);
                if(i++ >= maxRecords) {
                    break;
                }
            }
            retObject.put(key, jsonArray);
            retObject.put("lastDate", sdf.format(new Date(listingFullInfo.getTimestamp())));
        }
        restfulPage.setData(JSON.toJSONString(retObject));
        return JSON.toJSONString(restfulPage);
    }
}
