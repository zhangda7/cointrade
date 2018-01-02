package com.spare.cointrade.vendor.bithumb;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.spare.cointrade.model.depth.Tick;

import java.util.HashMap;

public class Main {
    public static void main(String args[]) {
		Api_Client api = new Api_Client("api connect key",
			"api secret key");
	
		HashMap<String, String> rgParams = new HashMap<String, String>();
//		rgParams.put("order_currency", "BTC");
//		rgParams.put("payment_currency", "KRW");
	
	
		try {
//		    String result = api.callApi("/info/balance", rgParams);
			String result = api.callApi("/public/ticker/ALL", rgParams);
		    System.out.println(result);
			parseTicker(result);
		} catch (Exception e) {
		    e.printStackTrace();
		}
		
    }

    private static TickerInfo parseTicker(String result) {
		JSONObject jsonObject = JSON.parseObject(result);
		if(! jsonObject.getString("status").equals("0000")) {
			return null;
		}
		TickerInfo tickerInfo = JSON.parseObject(jsonObject.getString("data"), TickerInfo.class);
		return tickerInfo;
	}
}

