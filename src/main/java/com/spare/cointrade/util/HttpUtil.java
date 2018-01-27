package com.spare.cointrade.util;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class HttpUtil {

    private static Logger logger = LoggerFactory.getLogger(HttpUtil.class);

    public static String LINE_SEPERATOR;

    static {
        LINE_SEPERATOR = System.getProperty("line.separator", "\r\n");
    }

    public static String doGet(String url) {
        InputStream in = null;
        BufferedReader reader = null;

        int retryTimes = 3;
        do {
            try {
                HttpClient httpClient = new HttpClient();
                httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(15 * 1000);
                httpClient.getHttpConnectionManager().getParams().setSoTimeout(15 * 1000);

                HttpMethod httpMethod = new GetMethod();
                int code = httpClient.executeMethod(httpMethod);
                if (code == HttpStatus.SC_OK) {
                    in = httpMethod.getResponseBodyAsStream();
                    reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line + LINE_SEPERATOR);
                    }

                    return stringBuilder.toString();
                }
            } catch (Exception e) {
                logger.error("ERROR on get {}", url, e);
            } finally {
                IOUtils.closeQuietly(reader);
                IOUtils.closeQuietly(in);
            }
        } while (--retryTimes > 0);

        return null;
    }


}
