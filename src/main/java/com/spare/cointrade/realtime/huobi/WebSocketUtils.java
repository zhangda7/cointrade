package com.spare.cointrade.realtime.huobi;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
//import com.spare.cointrade.actor.consumer.HuobiConsumer;
import com.spare.cointrade.model.*;
import com.spare.cointrade.model.depth.HuobiDepth;
import com.spare.cointrade.realtime.okcoin.model.OkcoinDepth;
import com.spare.cointrade.util.AkkaContext;
//import org.java_websocket.client.DefaultSSLWebSocketClientFactory;
import com.spare.cointrade.util.CoinTradeConstants;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebSocketUtils extends WebSocketClient {

	private static Logger logger = LoggerFactory.getLogger(WebSocketUtils.class);

//	private static final String url = "wss://be.huobi.com/ws";
	private static final String url = "wss://api.huobi.pro/ws";

	//FIXME 貌似比特币的只能和api.huobi.com交互
	private static final String BTC_DEPATH_STEP_TOPIC = "market.btccny.depth.step0";

	//FIXME 貌似ETH的只能和be.huobi.com交互
	private static final String ETH_DEPATH_STEP_TOPIC = "market.ethcny.depth.step0";

	private static final String ETH_BTC_DEPATH_STEP_TOPIC = "market.ethbtc.depth.step0";

	private static ActorSelection huobiConumser;

	private static ActorSelection tradeSel;

	private ActorSelection listingInfoMonitor;

	private static WebSocketUtils chatclient = null;

	private TradeType tradeType;

	private CoinType sourceCoinType;

	private CoinType targetCoinType;

	private String topic;

	private static void init() {
//		tradeSel = AkkaContext.getSystem().actorSelection("akka://rootSystem/user/tradeJudge");
		//		tradeJudge = AkkaContext.getSystem().actorSelection("user/tradeJudge").anchor();
//		huobiConumser = AkkaContext.getSystem().actorSelection("akka://rootSystem/user/huobiConsumer");

	}

	static {
		init();
	}

	public WebSocketUtils(URI serverUri, Draft draft) {
		super(serverUri, draft);
	}

	public WebSocketUtils(URI serverURI) {
		super(serverURI);
	}

	public WebSocketUtils(URI serverUri, Map<String, String> headers,
						  int connecttimeout, TradeType tradeType,
						  CoinType sourceType, CoinType targetType, String topic) {
		super(serverUri, new Draft_17(), headers, connecttimeout);
		this.tradeType = tradeType;
		this.sourceCoinType = sourceType;
		this.targetCoinType = targetType;
		this.topic = topic;
		listingInfoMonitor = AkkaContext.getSystem().actorSelection(
				AkkaContext.getFullActorName(CoinTradeConstants.ACTOR_LISTING_INFO_MONITOR));
	}

	@Override
	public void onOpen(ServerHandshake handshakedata) {
		try{
			logger.info("Huobi client--opened connection");
//			Future<ActorRef> actorRefFuture = AkkaContext.getSystem().actorSelection("akka://rootSystem/user/tradeJudge").resolveOne(Timeout.apply(5, TimeUnit.SECONDS));
//			tradeSel = AkkaContext.getSystem().actorSelection("akka://rootSystem/user/tradeJudge");
//			//		tradeJudge = AkkaContext.getSystem().actorSelection("user/tradeJudge").anchor();
//			huobiConumser = AkkaContext.getSystem().actorSelection("akka://rootSystem/user/huobiConsumer");
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
				logger.info("Huobi receive {}", depth);
				if(depth.getCh() != null && depth.getCh().equals(this.topic)) {
////					tradeJudge.tell(depth, ActorRef.noSender());
//					tradeSel.tell(depth, ActorRef.noSender());
//					huobiConumser.tell(depth, ActorRef.noSender());
					ListingFullInfo listingFullInfo = convert(depth.getCh(), depth);
					if(listingFullInfo != null) {
						listingInfoMonitor.tell(listingFullInfo, ActorRef.noSender());
					}
				}
			}
		} catch (IOException e) {
			logger.error("ERROR ", e);
		}
	}

	private ListingFullInfo convert(String channel, HuobiDepth huobiDepth) {
		if(huobiDepth == null ||
				(huobiDepth.getTick() == null) ) {
			return null;
		}
		ListingFullInfo listingFullInfo = new ListingFullInfo();
		listingFullInfo.setTradePlatform(TradePlatform.HUOBI);
		listingFullInfo.setTradeType(this.tradeType);
		if(! this.tradeType.equals(TradeType.COIN_COIN)) {
			throw new IllegalArgumentException("Trade type " + this.tradeType + "not supported");
		}
		listingFullInfo.setSourceCoinType(this.sourceCoinType);
		listingFullInfo.setTargetCoinType(this.targetCoinType);
		updateDepth(huobiDepth.getTick().getAsks(), listingFullInfo.getBuyDepth());
		updateDepth(huobiDepth.getTick().getBids(), listingFullInfo.getSellDepth());
		listingFullInfo.setTimestamp(huobiDepth.getTs());
		return listingFullInfo;
	}

	private ListingDepth updateDepth(List<List<Double>> depths, ListingDepth listingDepth) {
		if(depths == null) {
			return null;
		}
//		ListingDepth listingDepth = new ListingDepth();
		for (List<Double> one : depths) {
			ListingDepth.DepthInfo depthInfo = listingDepth.new DepthInfo();
			depthInfo.setPrice(one.get(0));
			depthInfo.setAmount(one.get(1));
			listingDepth.getDepthInfoMap().put(depthInfo.getPrice(), depthInfo);
		}
		return listingDepth;
	}

	@Override
	public void onMessage(String message) {
		System.out.println("接收--received: " + message);
	}

	public void onFragment(Framedata fragment) {
		logger.info("片段--received fragment: " + new String(fragment.getPayloadData().array()));
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
		HuobiDepth huobiDepth = new HuobiDepth();
		huobiDepth.setClear(true);
		tradeSel.tell(huobiDepth, ActorRef.noSender());
		logger.warn("关流--Connection closed by " + (remote ? "remote peer" : "us"));
//		try {
//			chatclient.close();
//		} catch (Exception e) {
//			logger.error("ERROR on close again ", e);
//		}
	}

	public static void closeQuietly() {
//		HuobiDepth huobiDepth = new HuobiDepth();
//		huobiDepth.setClear(true);
//		tradeSel.tell(huobiDepth, ActorRef.noSender());
//		logger.warn("关流--Connection closed by us");
//		try {
//			chatclient.close();
//			chatclient = null;
//		} catch (Exception e) {
//			logger.error("ERROR on close again ", e);
//		}
	}

	@Override
	public void onError(Exception ex) {
		logger.error("ERROR on huobi websockt", ex);
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
//			appClient.setWebSocketFactory(new DefaultSSLWebSocketClientFactory(sc));
//			sc.init( kmf.getKeyManagers(), tmf.getTrustManagers(), null );
			// sslContext.init( null, null, null ); // will use java's default key and trust store which is sufficient unless you deal with self-signed certificates

			SSLSocketFactory factory = sc.getSocketFactory();// (SSLSocketFactory) SSLSocketFactory.getDefault();

			chatclient.setSocket( factory.createSocket() );

//			chatclient.connectBlocking();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

//	static class TimerSend implements Runnable {
//
//		public TimerSend() {
//			try {
//				chatclient = new WebSocketUtils(new URI(url), getWebSocketHeaders(), 1000);
//				trustAllHosts(chatclient);
//
//				chatclient.connectBlocking();
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			} catch (URISyntaxException e) {
//				e.printStackTrace();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//
//		@Override
//		public void run() {
//			ReqModel reqModel1 = new ReqModel();
////			reqModel1.setReq("market.btccny.trade.detail");
//			reqModel1.setReq("market.btccny.detail");
//			reqModel1.setId(10004L);
//			chatclient.send(JSONObject.toJSONString(reqModel1));
//			logger.info("send : " + JSONObject.toJSONString(reqModel1));
//		}
//	}

	public static void executeWebSocket(TradeType tradeType, String topic, CoinType sourceType, CoinType targetType) throws Exception {
//		ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
//		executorService.scheduleWithFixedDelay(new TimerSend(), 1,1, TimeUnit.SECONDS);
//		Thread.currentThread().join();
		// WebSocketImpl.DEBUG = true;
		if(chatclient != null) {
			chatclient.close();
			chatclient = null;
		}
		chatclient = new WebSocketUtils(new URI(url), getWebSocketHeaders(), 1000, tradeType, sourceType, targetType, topic);
		trustAllHosts(chatclient);
		chatclient.connectBlocking();
//		// 订阅K线数据 sub 根据自己需要订阅数据
//		SubModel subModel = new SubModel();
//		subModel.setSub("market.btccny.kline.1min");
//		subModel.setId(10000L);
//		chatclient.send(JSONObject.toJSONString(subModel));
//
//		// 订阅数据深度
//		SubModel subModel1 = new SubModel();
//		subModel1.setSub(BTC_DEPATH_STEP_TOPIC);
//		subModel1.setId(10001L);
//		chatclient.send(JSONObject.toJSONString(subModel1));

        SubModel subModel1 = new SubModel();
        subModel1.setSub(topic);
//        subModel1.setSub(ETH_BTC_DEPATH_STEP_TOPIC);
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