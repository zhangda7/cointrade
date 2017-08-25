package com.spare.cointrade.dao;

import com.spare.cointrade.model.trade.HuobiTrade;
import com.spare.cointrade.model.trade.OkCoinTrade;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Created by dada on 2017/8/25.
 */
@Component
public class TradeEventMongoDao {

    @Autowired
    MongoDao mongoDao;

    public void insertTradeEvent(HuobiTrade huobiTrade) {
        Document document = new Document();
        document.put("source", "huobi");
        document.put("action", huobiTrade.getAction().getValue());
        document.put("type", huobiTrade.getSymbol());
        document.put("price", huobiTrade.getPrice());
        document.put("amount", huobiTrade.getAmount());
        document.put("ts", huobiTrade.getTs());
        document.put("comment", huobiTrade.getComment());
        document.put("gmtCreated", new Date());
        mongoDao.getMongoDatabase().getCollection("tradeEvent").insertOne(document);
    }

    public void insertTradeEvent(OkCoinTrade okCoinTrade) {
        Document document = new Document();
        document.put("source", "okcoin");
        document.put("action", okCoinTrade.getAction().getValue());
        document.put("type", okCoinTrade.getSymbol());
        document.put("price", okCoinTrade.getPrice());
        document.put("amount", okCoinTrade.getAmount());
        mongoDao.getMongoDatabase().getCollection("tradeEvent").insertOne(document);
    }

}
