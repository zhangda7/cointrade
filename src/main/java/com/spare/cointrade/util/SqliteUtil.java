//package com.spare.cointrade.util;
//
//import org.apache.commons.io.FileUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.File;
//import java.sql.*;
//
//public class SqliteUtil {
//
//    private static Logger logger = LoggerFactory.getLogger(SqliteUtil.class);
//
//    public synchronized static void createDbTableIfNotExsit() {
//
//    }
//
//    public static boolean checkTableIsExist(String tableName) {
//        try {
//            Class.forName("org.sqlite.JDBC");
//            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + SqliteUtil.DB_FILE);
//            String sql = "select count(*) as c from sqlite_master where type ='table' and name = ?";
//            PreparedStatement preparedStatement = connection.prepareStatement(sql);
//            preparedStatement.setString(1, tableName);
//            ResultSet ret = preparedStatement.executeQuery();
//            int count = ret.getInt("c");
//            return count == 1;
//        } catch (Exception e) {
//            logger.error("ERROR", e);
//        }
//        return false;
//    }
//
//
//}
