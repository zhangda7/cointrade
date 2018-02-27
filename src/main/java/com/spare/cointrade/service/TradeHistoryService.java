package com.spare.cointrade.service;

import com.spare.cointrade.ExchangeContext;
import com.spare.cointrade.model.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TradeHistoryService {

    private static Logger logger = LoggerFactory.getLogger(TradeHistoryService.class);

    public static TradeHistoryService INSTANCE = new TradeHistoryService();

    private Connection connection;

    private static final String DB_FILE = "/home/admin/data/coin.db";

    private TradeHistoryService() {
        try {
            File dbFile = new File(DB_FILE);
            if(! dbFile.exists()) {
                FileUtils.forceMkdir(dbFile.getParentFile());
                createTradeTable();
            }
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
        } catch (Exception e) {
            logger.error("ERROR ", e);
            System.exit(1);
        }

    }

    public int insert(TradeHistory tradeHistory) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateStr = sdf.format(new Date());
        PreparedStatement preparedStatement = null;
        try {
            String sql = "INSERT INTO trade_history (platform, action, coin_type, target_coin_type, price, amount," +
                    "result, account_name, pre_account_source_amount, after_account_source_amount," +
                    "pre_account_target_amount, after_account_target_amount, comment, gmt_created, gmt_modified, " +
                    "pairId, direction, profit, normalize_price, normalize_fee, trade_ts)\n" +
                    "VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            preparedStatement = this.connection.prepareStatement(sql);
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
            preparedStatement.setString(16, tradeHistory.getPairId());
            preparedStatement.setString(17, tradeHistory.getDirection());
            preparedStatement.setDouble(18, tradeHistory.getProfit());
            preparedStatement.setDouble(19, tradeHistory.getNormalizePrice());
            preparedStatement.setDouble(20, tradeHistory.getNormalizeFee());
            preparedStatement.setDouble(21, tradeHistory.getTradeTs());

            int ret = preparedStatement.executeUpdate();
            return ret;
        } catch ( Exception e ) {
            logger.error("ERROR ", e);
        } finally {
            if(preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    logger.error("ERROR ", e);
                }
            }
        }
        return 0;
    }

    public int updatePairResult(String pairId, String result) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS");
        String dateStr = sdf.format(new Date());
        PreparedStatement preparedStatement = null;
        try {
            String sql = "UPDATE trade_history SET result = ? WHERE pairId = ?";
            preparedStatement = this.connection.prepareStatement(sql);
            preparedStatement.setString(1, result);
            preparedStatement.setString(2, pairId);
            int ret = preparedStatement.executeUpdate();
            return ret;
        } catch ( Exception e ) {
            logger.error("ERROR ", e);
        } finally {
            if(preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    logger.error("ERROR ", e);
                }
            }
        }
        return 0;
    }

    public List<TradeHistory> listByDate(Long startTs, Long endTs) throws SQLException {
        List<TradeHistory> tradeHistoryList = new ArrayList<>();
        String selectSQL = "SELECT * FROM trade_history where trade_ts >= ? and trade_ts <= ? order by id desc limit 8000";
        PreparedStatement preparedStatement = this.connection.prepareStatement(selectSQL);
        preparedStatement.setLong(1, startTs);
        preparedStatement.setLong(2, endTs);

//        preparedStatement.setInt(1, 1001);
        ResultSet rs = preparedStatement.executeQuery();
        while (rs.next()) {
            TradeHistory tradeHistory = new TradeHistory();
            tradeHistory.setId(rs.getLong("id"));
            tradeHistory.setPairId(rs.getString("pairId"));
            tradeHistory.setDirection(rs.getString("direction"));
            tradeHistory.setProfit(rs.getDouble("profit"));
            tradeHistory.setTradePlatform(TradePlatform.valueOf(rs.getString("platform")));
            tradeHistory.setTradeAction(TradeAction.valueOf(rs.getString("action")));
            tradeHistory.setCoinType(CoinType.valueOf(rs.getString("coin_type")));
            tradeHistory.setTargetCoinType(CoinType.valueOf(rs.getString("target_coin_type")));
            tradeHistory.setPrice(rs.getDouble("price"));
            tradeHistory.setNormalizePrice(rs.getDouble("normalize_price"));
            tradeHistory.setAmount(rs.getDouble("amount"));
            tradeHistory.setResult(TradeResult.valueOf(rs.getString("result")));
            tradeHistory.setAccountName(rs.getString("account_name"));
            tradeHistory.setPreAccountSourceAmount(rs.getDouble("pre_account_source_amount"));
            tradeHistory.setAfterAccountSourceAmount(rs.getDouble("after_account_source_amount"));
            tradeHistory.setPreAccountTargetAmount(rs.getDouble("pre_account_target_amount"));
            tradeHistory.setAfterAccountTargetAmount(rs.getDouble("after_account_target_amount"));
            tradeHistory.setTradeTs(rs.getLong("trade_ts"));
            tradeHistory.setNormalizeFee(rs.getDouble("normalize_fee"));
            tradeHistory.setGmtCreated(rs.getString("gmt_created"));
            tradeHistoryList.add(tradeHistory);
        }
        rs.close();
        preparedStatement.close();
        return tradeHistoryList;
    }

//    public List<TradeHistory> list() throws SQLException {
//        List<TradeHistory> tradeHistoryList = new ArrayList<>();
//        String selectSQL = "SELECT * FROM trade_history order by id desc limit 400";
//        PreparedStatement preparedStatement = this.connection.prepareStatement(selectSQL);
////        preparedStatement.setInt(1, 1001);
//        ResultSet rs = preparedStatement.executeQuery();
//        while (rs.next()) {
//            TradeHistory tradeHistory = new TradeHistory();
//            tradeHistory.setId(rs.getLong("id"));
//            tradeHistory.setPairId(rs.getString("pairId"));
//            tradeHistory.setDirection(rs.getString("direction"));
//            tradeHistory.setProfit(rs.getDouble("profit"));
//            tradeHistory.setTradePlatform(TradePlatform.valueOf(rs.getString("platform")));
//            tradeHistory.setTradeAction(TradeAction.valueOf(rs.getString("action")));
//            tradeHistory.setCoinType(CoinType.valueOf(rs.getString("coin_type")));
//            tradeHistory.setTargetCoinType(CoinType.valueOf(rs.getString("target_coin_type")));
//            tradeHistory.setPrice(rs.getDouble("price"));
//            tradeHistory.setNormalizePrice(rs.getDouble("normalize_price"));
//            tradeHistory.setAmount(rs.getDouble("amount"));
//            tradeHistory.setResult(TradeResult.valueOf(rs.getString("result")));
//            tradeHistory.setAccountName(rs.getString("account_name"));
//            tradeHistory.setPreAccountSourceAmount(rs.getDouble("pre_account_source_amount"));
//            tradeHistory.setAfterAccountSourceAmount(rs.getDouble("after_account_source_amount"));
//            tradeHistory.setPreAccountTargetAmount(rs.getDouble("pre_account_target_amount"));
//            tradeHistory.setAfterAccountTargetAmount(rs.getDouble("after_account_target_amount"));
//            tradeHistory.setNormalizeFee(rs.getDouble("normalize_fee"));
//            tradeHistory.setGmtCreated(rs.getString("gmt_created"));
//            tradeHistoryList.add(tradeHistory);
//        }
//        rs.close();
//        preparedStatement.close();
//        return tradeHistoryList;
//    }

    private void createTradeTable() {
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
            System.out.println("Opened database successfully");
            stmt = c.createStatement();
            String sql = "CREATE TABLE trade_history " +
                    "(ID INTEGER PRIMARY KEY NOT NULL," +
                    " pairId TEXT, " +
                    " direction TEXT, " +
                    " platform TEXT, " +
                    " action TEXT, " +
                    " coin_type TEXT, " +
                    " target_coin_type TEXT," +
                    "price REAL, " +
                    "normalize_price REAL, " +
                    "amount REAL, " +
                    "result TEXT, " +
                    "profit REAL, " +
                    "account_name TEXT, " +
                    "pre_account_source_amount REAL, " +
                    "after_account_source_amount REAL, " +
                    "pre_account_target_amount REAL, " +
                    "after_account_target_amount REAL, " +
                    "comment REAL, " +
                    "normalize_fee REAL, " +
                    "trade_ts REAL, " +
                    "gmt_created TEXT, gmt_modified TEXT)";
            stmt.executeUpdate(sql);
            stmt.close();
            c.close();
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        logger.info("Table created successfully");
    }

}
