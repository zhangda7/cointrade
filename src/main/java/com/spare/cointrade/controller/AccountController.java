package com.spare.cointrade.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.spare.cointrade.model.Account;
import com.spare.cointrade.model.RestfulPage;
import com.spare.cointrade.account.AccountManager;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
public class AccountController {

    private static final int CODE_SUCCESS = 200;

    @RequestMapping("/accountInfo")
    public String accountInfo(@RequestParam("platform") String platform) {

        RestfulPage restfulPage = new RestfulPage();
        restfulPage.setCode(CODE_SUCCESS);

        JSONObject jsonObject = new JSONObject();
        for (Account account : AccountManager.INSTANCE.getPlatformAccountMap().values()) {
            if(! account.getTradePlatform().name().equals(platform)) {
                continue;
            }

            jsonObject.put("platform", account.getTradePlatform().name());
            jsonObject.put("coins", account.getBalanceMap().values());
        }
        restfulPage.setData(JSON.toJSONString(jsonObject));
        return JSON.toJSONString(restfulPage);
    }

}
