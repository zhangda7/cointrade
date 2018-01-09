package com.spare.cointrade.sqlite;

import com.spare.cointrade.model.*;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SQLiteJDBCTest
{
    public static void main( String args[] )
    {
        createTradeTable();
//        TradeHistory tradeHistory = new TradeHistory();
//        tradeHistory.setTradePlatform(TradePlatform.BITHUMB);
//        tradeHistory.setTradeAction(TradeAction.BUY);
//        tradeHistory.setCoinType(CoinType.BTC);
//        tradeHistory.setTargetCoinType(CoinType.QTUM);
//        tradeHistory.setPrice(100.02);
//        tradeHistory.setAmount(1.02);
//        tradeHistory.setResult(TradeResult.TRADING);
//        tradeHistory.setAccountName("hello");
//        tradeHistory.setPreAccountSourceAmount(2.01);
//        tradeHistory.setAfterAccountSourceAmount(0.01);
//        tradeHistory.setPreAccountTargetAmount(3.01);
//        tradeHistory.setAfterAccountTargetAmount(10.01);
//        tradeHistory.setComment("comment");
//        testInsert(tradeHistory);
    }

    private static void testConnection() {
        Connection c = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:coin.db");
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        System.out.println("Opened database successfully");
    }

    private static void testInsert(TradeHistory tradeHistory) {
        Connection c = null;
        Statement stmt = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateStr = sdf.format(new Date());
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:coin.db");
//            System.out.println("Opened database successfully");
//
//            stmt = c.createStatement();
//            String sql = "INSERT INTO trade_history (platform," +
//                    "action,coin_type,target_coin_type,amount," +
//                    "result,account_name,pre_account_amount,after_account_amount, gmt_created, gmt_modified)\n" +
//                    "VALUES ( '" + tradeHistory.getTradePlatform().name() + "', '" + tradeHistory.getTradeAction().name() + "', '" +
//                    tradeHistory.getCoinType().name() + "', '" + tradeHistory.getTargetCoinType().name() + "'," +
//                    tradeHistory.getAmount() + ", '" + tradeHistory.getResult() + "', '" +
//                    tradeHistory.getAccountName() + "', " + tradeHistory.getPreAccountAmount() + ", " + tradeHistory.getAfterAccountAmount() +
//                    ", '" + dateStr + "', '" + dateStr + "');";
//            System.out.println("Sql:" + sql);
//            stmt.executeUpdate(sql);
//            stmt.close();
            String sql = "INSERT INTO trade_history (platform, action, coin_type, target_coin_type, price, amount," +
                    "result, account_name, pre_account_source_amount, after_account_source_amount," +
                    "pre_account_target_amount, after_account_target_amount, comment, gmt_created, gmt_modified)\n" +
                    "VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement preparedStatement = c.prepareStatement(sql);
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
            c.close();
        } catch ( Exception e ) {
            e.printStackTrace();
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        System.out.println("Table created successfully");
    }

    private static void createTradeTable() {
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:coin.db");
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
                    "amount REAL, " +
                    "result TEXT, " +
                    "profit REAL, " +
                    "account_name TEXT, " +
                    "pre_account_source_amount REAL, " +
                    "after_account_source_amount REAL, " +
                    "pre_account_target_amount REAL, " +
                    "after_account_target_amount REAL, " +
                    "comment REAL, " +
                    "gmt_created TEXT, gmt_modified TEXT)";
            System.out.println("Sql:" + sql);
//            String sql = "CREATE TABLE COMPANY " +
//                    "(ID INT PRIMARY KEY     NOT NULL," +
//                    " NAME           TEXT, " +
//                    " AGE            INT, " +
//                    " ADDRESS        CHAR(50), " +
//                    " SALARY         REAL)";
            stmt.executeUpdate(sql);
            stmt.close();
            c.close();
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        System.out.println("Table created successfully");
    }
}