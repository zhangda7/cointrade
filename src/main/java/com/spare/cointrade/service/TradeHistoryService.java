package com.spare.cointrade.service;

import com.spare.cointrade.ExchangeContext;
import com.spare.cointrade.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TradeHistoryService {

    private static Logger logger = LoggerFactory.getLogger(TradeHistoryService.class);

    public static TradeHistoryService INSTANCE = new TradeHistoryService();

    private Connection connection;

    private TradeHistoryService() {
        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection("jdbc:sqlite:coin.db");
        } catch (Exception e) {
            logger.error("ERROR ", e);
            System.exit(1);
        }

    }

    public int insert(TradeHistory tradeHistory) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS");
        String dateStr = sdf.format(new Date());
        try {
            String sql = "INSERT INTO trade_history (platform, action, coin_type, target_coin_type, price, amount," +
                    "result, account_name, pre_account_source_amount, after_account_source_amount," +
                    "pre_account_target_amount, after_account_target_amount, comment, gmt_created, gmt_modified)\n" +
                    "VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement preparedStatement = this.connection.prepareStatement(sql);
            preparedStatement.setString(1, tradeHistory.getTradePlatform().name());
            preparedStatement.setString(2, tradeHistory.getTradeAction().name());
            preparedStatement.setString(3, tradeHistory.getCoinType().name());
            preparedStatement.setString(4, tradeHistory.getTargetCoinType().name());
            preparedStatement.setDouble(5, tradeHistory.getPrice());
            preparedStatement.setDouble(6, tradeHistory.getAmount());
            preparedStatement.setString(7, tradeHistory.getResult().name());
            preparedStatement.setString(8, tradeHistory.getAccountName());
            preparedStatement.setDouble(9, tradeHistory.getPreAccountSourceAmount());
            preparedStatement.setDouble(10, tradeHistory.getAfterAccountSourceAmount());
            preparedStatement.setDouble(11, tradeHistory.getPreAccountTargetAmount());
            preparedStatement.setDouble(12, tradeHistory.getAfterAccountTargetAmount());
            preparedStatement.setString(13, tradeHistory.getComment());
            preparedStatement.setString(14, dateStr);
            preparedStatement.setString(15, dateStr);
            int ret = preparedStatement.executeUpdate();
            return ret;
        } catch ( Exception e ) {
            logger.error("ERROR ", e);
        }
        return 0;
    }

    public List<TradeHistory> list() throws SQLException {
        List<TradeHistory> tradeHistoryList = new ArrayList<>();
        String selectSQL = "SELECT * FROM trade_history order by id desc limit 200";
        PreparedStatement preparedStatement = this.connection.prepareStatement(selectSQL);
//        preparedStatement.setInt(1, 1001);
        ResultSet rs = preparedStatement.executeQuery();
        while (rs.next()) {
            TradeHistory tradeHistory = new TradeHistory();
            tradeHistory.setId(rs.getLong("id"));
            tradeHistory.setTradePlatform(TradePlatform.valueOf(rs.getString("platform")));
            tradeHistory.setTradeAction(TradeAction.valueOf(rs.getString("action")));
            tradeHistory.setCoinType(CoinType.valueOf(rs.getString("coin_type")));
            tradeHistory.setTargetCoinType(CoinType.valueOf(rs.getString("target_coin_type")));
            tradeHistory.setPrice(rs.getDouble("price"));
            if(tradeHistory.getTradePlatform().equals(TradePlatform.BITHUMB)) {
                tradeHistory.setNormalizePrice(tradeHistory.getPrice() * ExchangeContext.KRW2CNY());
            } else if(tradeHistory.getTradePlatform().equals(TradePlatform.BINANCE)){
                tradeHistory.setNormalizePrice(tradeHistory.getPrice() * ExchangeContext.USD2CNY());
            }
            tradeHistory.setAmount(rs.getDouble("amount"));
            tradeHistory.setResult(TradeResult.valueOf(rs.getString("result")));
            tradeHistory.setAccountName(rs.getString("account_name"));
            tradeHistory.setPreAccountSourceAmount(rs.getDouble("pre_account_source_amount"));
            tradeHistory.setAfterAccountSourceAmount(rs.getDouble("after_account_source_amount"));
            tradeHistory.setPreAccountTargetAmount(rs.getDouble("pre_account_target_amount"));
            tradeHistory.setAfterAccountTargetAmount(rs.getDouble("after_account_target_amount"));
            tradeHistory.setGmtCreated(rs.getString("gmt_created"));
            tradeHistoryList.add(tradeHistory);
        }
        rs.close();
        preparedStatement.close();
        return tradeHistoryList;
    }

}
