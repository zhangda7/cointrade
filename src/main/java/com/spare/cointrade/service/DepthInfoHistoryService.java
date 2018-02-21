package com.spare.cointrade.service;

import com.spare.cointrade.model.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DepthInfoHistoryService {

    private static Logger logger = LoggerFactory.getLogger(DepthInfoHistoryService.class);

    public static DepthInfoHistoryService INSTANCE = new DepthInfoHistoryService();

    private Connection connection;

    public static final String DB_FILE = "/home/admin/data/depthHistory.db";

    private DepthInfoHistoryService() {
        try {
            File dbFile = new File(DB_FILE);
            if(! dbFile.exists()) {
                FileUtils.forceMkdir(dbFile.getParentFile());
                createTradeDepthInfo();
            }
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
        } catch (Exception e) {
            logger.error("ERROR", e);
            System.exit(1);
        }
    }

    public int insert(DepthInfoHistory depthInfoHistory) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateStr = sdf.format(new Date());
        PreparedStatement preparedStatement = null;
        try {
            String sql = "INSERT INTO depth_info_history (platform, source_coin, target_coin, " +
                    "ori_bid_price1, normalize_bid_price1, bid_amount1," +
                    "ori_ask_price1, normalize_ask_price1, ask_amount1, " +
                    "sample_ts, gmt_created, gmt_modified)\n" +
                    "VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            preparedStatement = this.connection.prepareStatement(sql);
            preparedStatement.setString(1, depthInfoHistory.getPlatform().name());
            preparedStatement.setString(2, depthInfoHistory.getSourceCoin().name());
            preparedStatement.setString(3, depthInfoHistory.getTargetCoin().name());
            preparedStatement.setDouble(4, depthInfoHistory.getOriBidPrice1());
            preparedStatement.setDouble(5, depthInfoHistory.getNormalizeBidPrice1());
            preparedStatement.setDouble(6, depthInfoHistory.getBidAmount1());
            preparedStatement.setDouble(7, depthInfoHistory.getOriAskPrice1());
            preparedStatement.setDouble(8, depthInfoHistory.getNormalizeAskPrice1());
            preparedStatement.setDouble(9, depthInfoHistory.getAskAmount1());
            preparedStatement.setDouble(10, depthInfoHistory.getSampleTs());
            preparedStatement.setString(11, dateStr);
            preparedStatement.setString(12, dateStr);

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

//    public List<TradeHistory> listByDate(Long startTs, Long endTs) throws SQLException {
//        List<TradeHistory> tradeHistoryList = new ArrayList<>();
//        String selectSQL = "SELECT * FROM trade_history where trade_ts >= ? and trade_ts <= ? order by id desc limit 400";
//        PreparedStatement preparedStatement = this.connection.prepareStatement(selectSQL);
//        preparedStatement.setLong(1, startTs);
//        preparedStatement.setLong(2, endTs);
//
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
//            tradeHistory.setTradeTs(rs.getLong("trade_ts"));
//            tradeHistory.setNormalizeFee(rs.getDouble("normalize_fee"));
//            tradeHistory.setGmtCreated(rs.getString("gmt_created"));
//            tradeHistoryList.add(tradeHistory);
//        }
//        rs.close();
//        preparedStatement.close();
//        return tradeHistoryList;
//    }

    private static void createTradeDepthInfo() {
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
            System.out.println("Opened database coindb successfully");
            stmt = c.createStatement();
            String sql = "CREATE TABLE depth_info_history " +
                    "(ID INTEGER PRIMARY KEY NOT NULL," +
                    " platform TEXT, " +
                    " source_coin TEXT, " +
                    " target_coin TEXT," +
                    "ori_bid_price1 REAL, " +
                    "normalize_bid_price1 REAL, " +
                    "bid_amount1 REAL, " +
                    "ori_ask_price1 REAL, " +
                    "normalize_ask_price1 REAL, " +
                    "ask_amount1 REAL, " +
                    "sample_ts REAL, " +
                    "gmt_created TEXT, gmt_modified TEXT)";

            stmt.executeUpdate(sql);
            stmt.close();
            c.close();
        } catch ( Exception e ) {
            logger.error("ERROR", e);
            System.exit(0);
        }
        logger.info("Table depth_info_history created successfully");
    }

}
