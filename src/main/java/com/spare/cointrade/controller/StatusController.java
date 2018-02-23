package com.spare.cointrade.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.spare.cointrade.actor.monitor.ListingInfoMonitor;
import com.spare.cointrade.actor.trade.TradeJudge;
import com.spare.cointrade.actor.trade.TradeJudgeV2;
import com.spare.cointrade.actor.trade.TradeJudgeV3;
import com.spare.cointrade.model.*;
import com.spare.cointrade.service.DepthInfoHistoryService;
import com.spare.cointrade.service.TradeHistoryService;
import com.spare.cointrade.trade.okcoin.StringUtil;
import com.spare.cointrade.util.TradeConfigContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

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

    @RequestMapping("/monitorStatus")
    public String listingMonitorStatus() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        RestfulPage restfulPage = new RestfulPage();
        restfulPage.setCode(CODE_SUCCESS);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("totalNomalizeProfit", String.valueOf(TradeConfigContext.getINSTANCE().getTotalProfit()));
        jsonObject.put("totalNomalizeFee", String.valueOf(TradeConfigContext.getINSTANCE().getTotalFee()));

        JSONArray historyList = new JSONArray();
        for (OrderBookHistory orderBookHistory : TradeConfigContext.getINSTANCE().getOrderBookHistoryMap().values()) {
            JSONObject history = new JSONObject();
            history.put("coinType", orderBookHistory.getCoinType());
            history.put("totalProfit", orderBookHistory.getTotalProfit());
            history.put("totalFee", orderBookHistory.getTotalFee());
            history.put("totalAmount", orderBookHistory.getTotalAmount());
            history.put("averageProfit", orderBookHistory.getAverageProfit());
            history.put("updateDate", sdf.format(new Date(orderBookHistory.getUpdateTs())));
            historyList.add(history);
        }
        jsonObject.put("profitHistoryList", historyList);
        restfulPage.setData(JSON.toJSONString(jsonObject));
        return JSON.toJSONString(restfulPage);
    }

    @RequestMapping("/listingPriceDelta")
    public String listingPriceDelta() {
        RestfulPage restfulPage = new RestfulPage();
        restfulPage.setCode(CODE_SUCCESS);
//        restfulPage.setData(JSON.toJSONString(TradeJudgeV3.chanceTradeMap.values()));
        TreeMap<String, OrderBookEntry> sorted = new TreeMap<>();
        sorted.putAll(TradeJudgeV3.chanceTradeMap);

        restfulPage.setData(JSON.toJSONString(sorted));
        return JSON.toJSONString(restfulPage);
    }

    @RequestMapping("/listingTradeHistory")
    public String listingTradeHistory(@RequestParam(value = "tradePlatform", required = false) String platform,
                                      @RequestParam(value = "page", required = false) Integer page,
                                      @RequestParam(value = "limit", required = false) Integer limit,
                                      @RequestParam(value = "direction", required = false) String direction,
                                      @RequestParam(value = "startTime", required = false) String startTime,
                                      @RequestParam(value = "endTime", required = false) String endTime) throws ParseException {
        RestfulPage restfulPage = new RestfulPage();
        restfulPage.setCode(CODE_SUCCESS);
        long endTs = System.currentTimeMillis();
        long startTs = endTs - 24 * 3600 * 1000;
        if(! (StringUtils.isEmpty(startTime) || StringUtils.isEmpty(endTime))) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date startDate = sdf.parse(startTime);
            Date endDate = sdf.parse(endTime);
            startTs = startDate.getTime();
            endTs = endDate.getTime();
            logger.info("Receive time query [{} {}] [{} {}]", startTime, startDate, endTime, endDate);
        }
        try {
            List<TradeHistory> tradeHistoryList = TradeHistoryService.INSTANCE.listByDate(startTs, endTs);
            Double totalProfit = 0.0;
            Double totalFee = 0.0;
            Set<String> handledPaidId = new HashSet<>();
            Integer tradeCount = 0;
            for (TradeHistory tradeHistory : tradeHistoryList) {
                totalFee += tradeHistory.getNormalizeFee();
                if(handledPaidId.contains(tradeHistory.getPairId())) {
                    continue;
                }
                tradeCount++;
                totalProfit += tradeHistory.getProfit();
                handledPaidId.add(tradeHistory.getPairId());
            }
//            if(platform != null && ! platform.equalsIgnoreCase("UNDEFINED")) {
            List<TradeHistory> newList = new ArrayList<>();
            int skip = 0;
            if(limit == null) {
                limit = 30;
            }
            if(page != null) {
                skip = limit * (page - 1);
            }
            int index = 0;
            for (TradeHistory tradeHistory : tradeHistoryList) {
                if(!StringUtils.isEmpty(platform)) {
                    if(! tradeHistory.getTradePlatform().name().equals(platform)) {
                        continue;
                    }
                }
                if(! StringUtils.isEmpty(direction)) {
                    if(! tradeHistory.getDirection().equals(direction)) {
                        continue;
                    }
                }
                if(index++ < skip) {
                    continue;
                }
                if(newList.size() >= limit) {
                    break;
                }

                newList.add(tradeHistory);
            }
            restfulPage.setCount(tradeHistoryList.size());
//            restfulPage.setData(JSON.toJSONString(newList));
//            }
            JSONObject data = new JSONObject();
            data.put("tableData", JSON.toJSONString(newList));

            JSONObject stat = new JSONObject();
            stat.put("totalProfit", totalProfit);
            stat.put("totalFee", totalFee);
            stat.put("totalCount", tradeCount);

            data.put("stat", stat);
            restfulPage.setData(JSON.toJSONString(data));

            return JSON.toJSONString(restfulPage);
        } catch (Exception e) {
            logger.error("ERROR ", e);
        }
        restfulPage.setCode(CODE_FAIL);
        restfulPage.setData("");
        return JSON.toJSONString(restfulPage);
    }

    @RequestMapping("/listingDepthHistory")
    public String listingDepthHistory(
            @RequestParam(value = "sourceCoin") String sourceCoin,
            @RequestParam(value = "startTime") String startTime,
            @RequestParam(value = "endTime") String endTime) {
        RestfulPage restfulPage = new RestfulPage();
        restfulPage.setCode(CODE_SUCCESS);

        try{
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            SimpleDateFormat simpleSdf = new SimpleDateFormat("dd HH:mm:ss");
            Date startDate = sdf.parse(startTime);
            Date endDate = sdf.parse(endTime);
            long startTs = startDate.getTime();
            long endTs = endDate.getTime();
            logger.info("Receive time query [{} {}] [{} {}]", startTime, startDate, endTime, endDate);
            List<DepthInfoHistory> depthInfoHistoryList = DepthInfoHistoryService.INSTANCE.list(sourceCoin, startTs, endTs);
            JSONObject data = new JSONObject();
            JSONArray xAsis = new JSONArray();
            JSONArray binance = new JSONArray();
            JSONArray bithumb = new JSONArray();
            JSONArray delta = new JSONArray();
            Map<Long, List<DepthInfoHistory>> tmpMap = new TreeMap<>();
            for(DepthInfoHistory depthInfoHistory : depthInfoHistoryList) {
                if(! tmpMap.containsKey(depthInfoHistory.getSampleTs())) {
                    tmpMap.put(depthInfoHistory.getSampleTs(), new ArrayList<>());
                }
                tmpMap.get(depthInfoHistory.getSampleTs()).add(depthInfoHistory);
            }
            for (Map.Entry<Long, List<DepthInfoHistory>> entry : tmpMap.entrySet()) {
                if(entry.getValue().size() < 2) {
                    //这里可能会出来3个的，就是一个币种有多种组合的情况。后面再处理这种展示
                    continue;
                }
                xAsis.add(simpleSdf.format(new Date(entry.getKey())));
                for (DepthInfoHistory depthInfoHistory : entry.getValue()) {
                    if(depthInfoHistory.getPlatform().equals(TradePlatform.BINANCE)) {
                        binance.add(depthInfoHistory.getNormalizeAskPrice1());
                    } else if(depthInfoHistory.getPlatform().equals(TradePlatform.BITHUMB)) {
                        bithumb.add(depthInfoHistory.getNormalizeAskPrice1());
                    }
                }
                delta.add((Double)(binance.get(binance.size() - 1)) - (Double)(bithumb.get(bithumb.size() - 1)));
            }
            data.put("xaxis", xAsis);
            data.put("binance", binance);
            data.put("bithumb", bithumb);
            data.put("delta", delta);
            data.put("count", xAsis.size());
            restfulPage.setData(JSON.toJSONString(data));

        } catch (Exception e) {
            logger.error("ERROR ", e);
        }

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
            retObject.put("lastRequestDate", sdf.format(new Date(listingFullInfo.getRequestTs())));
        }
        restfulPage.setData(JSON.toJSONString(retObject));
        return JSON.toJSONString(restfulPage);
    }
}
