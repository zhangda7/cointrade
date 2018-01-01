package com.spare.cointrade.controller;

import akka.actor.ActorRef;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.spare.cointrade.actor.monitor.ListingInfoMonitor;
import com.spare.cointrade.actor.trade.TradeJudge;
import com.spare.cointrade.actor.trade.TradeJudgeV2;
import com.spare.cointrade.model.ListingDepth;
import com.spare.cointrade.model.ListingFullInfo;
import com.spare.cointrade.model.RestfulPage;
import com.spare.cointrade.model.TradePlatform;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by dada on 2017/7/16.
 */
@CrossOrigin(origins = "*")
@RestController
public class StatusController {

    private static final int CODE_SUCCESS = 200;

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
        return listingBuyInfo(TradePlatform.OKEX);
    }

    @RequestMapping("/listingHuobiInfo")
    public String listingHuobiInfo() {
        return listingBuyInfo(TradePlatform.HUOBI);
    }


    public String listingBuyInfo(TradePlatform platform) {
        RestfulPage restfulPage = new RestfulPage();
        restfulPage.setCode(CODE_SUCCESS);
        JSONObject retObject = new JSONObject();
        int maxRecords = 5;
        for (ListingFullInfo listingFullInfo : ListingInfoMonitor.listingFullInfoMap.values()) {
            if(! listingFullInfo.getTradePlatform().equals(platform)) {
                continue;
            }
            String key = listingFullInfo.toKey();
            JSONArray jsonArray = new JSONArray();
            int i = 1;
            for(ListingDepth.DepthInfo depthInfo : listingFullInfo.getBuyDepth().getDepthInfoMap().values()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", "卖(" + i + ")");
                jsonObject.put("price", depthInfo.getPrice());
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
                jsonObject.put("price", depthInfo.getPrice());
                jsonObject.put("amount", depthInfo.getAmount());
                jsonObject.put("type", "buy");
                jsonArray.add(jsonObject);
                if(i++ >= maxRecords) {
                    break;
                }
            }
            retObject.put(key, jsonArray);
        }
        restfulPage.setData(JSON.toJSONString(retObject));
        return JSON.toJSONString(restfulPage);
    }
}
