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

    @RequestMapping("/listingBuyInfo")
    public String listingBuyInfo() {
        RestfulPage restfulPage = new RestfulPage();
        restfulPage.setCode(CODE_SUCCESS);
        JSONObject retObject = new JSONObject();
        int maxRecords = 5;
        for (ListingFullInfo listingFullInfo : ListingInfoMonitor.listingFullInfoMap.values()) {
            String key = listingFullInfo.toKey();
            JSONArray jsonArray = new JSONArray();
            int i = 0;
            for(ListingDepth.DepthInfo depthInfo : listingFullInfo.getBuyDepth().getDepthInfoMap().values()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("price", depthInfo.getPrice());
                jsonObject.put("amount", depthInfo.getAmount());
                jsonArray.add(jsonObject);
                if(i++ > maxRecords) {
                    break;
                }
            }
            retObject.put(key, jsonArray);
        }
        restfulPage.setData(JSON.toJSONString(retObject));
        return JSON.toJSONString(restfulPage);
    }
}
