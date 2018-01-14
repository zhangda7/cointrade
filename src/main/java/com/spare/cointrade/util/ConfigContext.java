package com.spare.cointrade.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.spare.cointrade.model.Ewma;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;

@Component
public class ConfigContext {

    private static Logger logger = LoggerFactory.getLogger(ConfigContext.class);

    private Double totalProfit = 0.0;

    private Double normalizeProfit = 0.0;

    public synchronized void addProfit(Double profit) {
        totalProfit += profit;
    }

    private static ConfigContext INSTANCE;

    @PostConstruct
    private void init() {
        INSTANCE = this;
        File configFile = new File("/home/admin/conf/coin-config");
        if(! configFile.exists()) {
            printConfig();
            return;
        }
        try {
            String content = readFile(configFile);
            JSONObject jsonObject = JSON.parseObject(content);
            if(jsonObject.containsKey("totalProfit")) {
                totalProfit = jsonObject.getDouble("totalProfit");
            }
            if(jsonObject.containsKey("normalizeProfit")) {
                normalizeProfit = jsonObject.getDouble("normalizeProfit");
            }
            printConfig();
        } catch (IOException e) {
            logger.error("ERROR ", e);
            System.exit(1);
        }

    }

    private void updateFile(File file, String content) {
        try {
            file.createNewFile(); // 创建新文件
            BufferedWriter out = new BufferedWriter(new FileWriter(file));
            out.write(content); // \r\n即为换行
            out.flush(); // 把缓存区内容压入文件
            out.close(); // 最后记得关闭文件
        } catch (Exception e) {
            logger.error("ERROR on write file {}", e);
        }
    }

    private void printConfig() {
        logger.info("{}", StringUtils.repeat("=", 50));
        logger.info("Read Config context");
        logger.info("normalizeProfit {}", normalizeProfit);
        logger.info("totalProfit {}", totalProfit);
        logger.info("{}", StringUtils.repeat("=", 50));

    }

    public static ConfigContext getINSTANCE() {
        return INSTANCE;
    }

    private String readFile(File file) throws IOException {
        InputStreamReader reader = new InputStreamReader(
                new FileInputStream(file)); // 建立一个输入流对象reader
        BufferedReader br = new BufferedReader(reader); // 建立一个对象，它把文件内容转成计算机能读懂的语言
        StringBuilder stringBuilder = new StringBuilder();
        String line = "";
        line = br.readLine();
        while (line != null) {
            line = br.readLine(); // 一次读入一行数据
            stringBuilder.append(line.trim());
        }
        return stringBuilder.toString();
    }

    public Double getTotalProfit() {
        return totalProfit;
    }

    public void setTotalProfit(Double totalProfit) {
        this.totalProfit = totalProfit;
    }

    public Double getNormalizeProfit() {
        return normalizeProfit;
    }

    public void setNormalizeProfit(Double normalizeProfit) {
        this.normalizeProfit = normalizeProfit;
    }
}
