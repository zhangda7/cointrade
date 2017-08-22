package com.spare.cointrade.realtime.huobi;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.spare.cointrade.actor.consumer.HuobiConsumer;
import com.spare.cointrade.model.HuobiDepth;
import com.spare.cointrade.util.AkkaContext;
import org.java_websocket.client.DefaultSSLWebSocketClientFactory;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

public class WebSocketUtils extends WebSocketClient {

	private static Logger logger = LoggerFactory.getLogger(WebSocketUtils.class);

//	private static final String url = "wss://be.huobi.com/ws";
	private static final String url = "wss://api.huobi.com/ws";

	private static final String BTC_DEPATH_STEP_TOPIC = "market.btccny.depth.step0";

	private ActorRef huobiConumser;

	private ActorRef tradeJudge;

	private ActorSelection tradeSel;

	private static WebSocketUtils chatclient = null;

	public WebSocketUtils(URI serverUri, Draft draft) {
		super(serverUri, draft);
	}

	public WebSocketUtils(URI serverURI) {
		super(serverURI);
	}

	public WebSocketUtils(URI serverUri, Map<String, String> headers, int connecttimeout) {
		super(serverUri, new Draft_17(), headers, connecttimeout);
	}

	@Override
	public void onOpen(ServerHandshake handshakedata) {
		try{
			logger.info("Huobi client--opened connection");
//			Future<ActorRef> actorRefFuture = AkkaContext.getSystem().actorSelection("akka://rootSystem/user/tradeJudge").resolveOne(Timeout.apply(5, TimeUnit.SECONDS));
			tradeSel = AkkaContext.getSystem().actorSelection("akka://rootSystem/user/tradeJudge");
			//		tradeJudge = AkkaContext.getSystem().actorSelection("user/tradeJudge").anchor();
			huobiConumser = AkkaContext.getSystem().actorOf(HuobiConsumer.props(), "huobiConsumer");
//		tradeJudge = AkkaContext.getSystem().actorOf(TradeJudge.props(), "tradeJudge");
		} catch (Exception e) {
			logger.error("ERROR ", e);
		}

	}

	@Override
	public void onMessage(ByteBuffer socketBuffer) {
		try {
			String marketStr = CommonUtils.byteBufferToString(socketBuffer);
			String market = CommonUtils.uncompress(marketStr);
			if (market.contains("ping")) {
//				System.out.println(market.replace("ping", "pong"));
				// Client 心跳
				chatclient.send(market.replace("ping", "pong"));
			} else {
//				System.out.println(" market:" + market);
				HuobiDepth depth = JSON.parseObject(market, HuobiDepth.class);
				if(depth.getCh().equals(BTC_DEPATH_STEP_TOPIC)) {
//					tradeJudge.tell(depth, ActorRef.noSender());
					tradeSel.tell(depth, ActorRef.noSender());
					huobiConumser.tell(depth, ActorRef.noSender());
				}
			}
		} catch (IOException e) {
			logger.error("ERROR ", e);
		}
	}

	@Override
	public void onMessage(String message) {
		System.out.println("接收--received: " + message);
	}

	public void onFragment(Framedata fragment) {
		System.out.println("片段--received fragment: " + new String(fragment.getPayloadData().array()));
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
		System.out.println("关流--Connection closed by " + (remote ? "remote peer" : "us"));
	}

	@Override
	public void onError(Exception ex) {
		System.out.println("WebSocket 连接异常: " + ex);
	}

	public static Map<String, String> getWebSocketHeaders() throws IOException {
		Map<String, String> headers = new HashMap<String, String>();
		return headers;
	}

	private static void trustAllHosts(WebSocketUtils appClient) {
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[] {};
			}

			public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}

			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}
		} };

		try {
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			appClient.setWebSocketFactory(new DefaultSSLWebSocketClientFactory(sc));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static class TimerSend implements Runnable {

		public TimerSend() {
			try {
				chatclient = new WebSocketUtils(new URI(url), getWebSocketHeaders(), 1000);
				trustAllHosts(chatclient);

				chatclient.connectBlocking();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			ReqModel reqModel1 = new ReqModel();
//			reqModel1.setReq("market.btccny.trade.detail");
			reqModel1.setReq("market.btccny.detail");
			reqModel1.setId(10004L);
			chatclient.send(JSONObject.toJSONString(reqModel1));
			logger.info("send : " + JSONObject.toJSONString(reqModel1));
		}
	}

	public static void executeWebSocket() throws Exception {
//		ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
//		executorService.scheduleWithFixedDelay(new TimerSend(), 1,1, TimeUnit.SECONDS);
//		Thread.currentThread().join();
		// WebSocketImpl.DEBUG = true;
		chatclient = new WebSocketUtils(new URI(url), getWebSocketHeaders(), 1000);
		trustAllHosts(chatclient);
		chatclient.connectBlocking();
//		// 订阅K线数据 sub 根据自己需要订阅数据
//		SubModel subModel = new SubModel();
//		subModel.setSub("market.btccny.kline.1min");
//		subModel.setId(10000L);
//		chatclient.send(JSONObject.toJSONString(subModel));
//
//		// 订阅数据深度
		SubModel subModel1 = new SubModel();
		subModel1.setSub(BTC_DEPATH_STEP_TOPIC);
		subModel1.setId(10001L);
		chatclient.send(JSONObject.toJSONString(subModel1));
//		// 取消订阅省略
//
//		// 请求数据 sub 根据自己需要请求数据
//		ReqModel reqModel = new ReqModel();
//		reqModel.setReq("market.btccny.depth.percent10");
//		reqModel.setId(10002L);
//		chatclient.send(JSONObject.toJSONString(reqModel));

		// 请求数据
//		ReqModel reqModel1 = new ReqModel();
//		reqModel1.setReq("market.btccny.detail");
//		reqModel1.setId(10003L);
//		chatclient.send(JSONObject.toJSONString(reqModel1));
//		System.out.println("send : " + JSONObject.toJSONString(reqModel1));

//		ReqModel reqModel1 = new ReqModel();
//		reqModel1.setReq("market.btccny.trade.detail");
//		reqModel1.setId(10004L);
//		chatclient.send(JSONObject.toJSONString(reqModel1));
//		System.out.println("send : " + JSONObject.toJSONString(reqModel1));
	}
}