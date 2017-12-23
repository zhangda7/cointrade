package com.spare.cointrade.realtime.okcoin;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by dada on 2017/8/20.
 */
@Component
public class OkcoinClient {

//    @PostConstruct
    public void startFetch() {
        // 国际站WebSocket地址 注意如果访问国内站 请将 real.okcoin.com 改为 real.okcoin.cn
//        String url = "wss://real.okcoin.cn:10440/websocket/okcoinapi";
//        String url = "wss://real.okex.com:10440/websocket/okexapi"; // OKEx合约
        String url = "wss://real.okex.com:10441/websocket"; // OKEx币币
        // 订阅消息处理类,用于处理WebSocket服务返回的消息
        WebSocketService service = new BuissnesWebSocketServiceImpl();

        // WebSocket客户端
        WebSoketClient client = new WebSoketClient(url, service);

        // 启动客户端
        client.start();

        // 添加订阅
//		client.addChannel("ok_sub_spotusd_btc_ticker");

//        client.addChannel("ok_sub_spot_btc_depth");

//        client.addChannel("ok_sub_spot_eth_depth");
        client.addChannel("ok_sub_spot_bch_btc_depth");
    }

}
