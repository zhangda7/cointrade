package com.spare.cointrade.account.impl;

import com.spare.cointrade.account.IAccountService;
import com.spare.cointrade.model.Account;
import com.spare.cointrade.vendor.bithumb.Api_Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class BithumbAccountService implements IAccountService{

    private static Logger logger = LoggerFactory.getLogger(BithumbAccountService.class);

    private Api_Client apiClient;

    @Value("${BITHUMB_AK}")
    private String appkey;

    @Value("${BITHUMB_AS}")
    private String secret;

    public BithumbAccountService() {

    }

    @PostConstruct
    public void init() {
        apiClient = new Api_Client(appkey,
                secret);
    }

    @Override
    public Account fetchBalance() {
        String result = this.apiClient.fetchBalance();
        logger.info("Bithumb account info is {}", result);
        return null;
    }

    public String getAppkey() {
        return appkey;
    }

    public void setAppkey(String appkey) {
        this.appkey = appkey;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
