package com.spare.cointrade.realtime.okcoin;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.spare.cointrade.model.*;
import com.spare.cointrade.realtime.okcoin.model.OkcoinDepth;
import com.spare.cointrade.util.AkkaContext;
import com.spare.cointrade.util.CoinTradeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.List;

/**
 * 订阅信息处理类需要实现WebSocketService接口
 * @author okcoin
 *
 */
public class BuissnesWebSocketServiceImpl implements WebSocketService {
	private Logger log = LoggerFactory.getLogger(BuissnesWebSocketServiceImpl.class);
	private static final Type type = new TypeReference<List<OkCoinData>>() {}.getType();

//	private ActorSelection tradeJudge;
	private ActorSelection listingInfoMonitor;

	private TradeType tradeType;

	public BuissnesWebSocketServiceImpl(TradeType tradeType) {
//		tradeJudge = AkkaContext.getSystem().actorSelection("akka://rootSystem/user/tradeJudge");
		listingInfoMonitor = AkkaContext.getSystem().actorSelection(
				AkkaContext.getFullActorName(CoinTradeConstants.ACTOR_LISTING_INFO_MONITOR));
		this.tradeType = tradeType;
	}

	@Override
	public void onReceive(String msg) {
		if(msg.equals("{\"event\":\"pong\"}")) {
			return;
		}
		log.info("WebSocket Client received message: " + msg);
		List<OkCoinData> okCoinDataList = JSON.parseObject(msg, type);
		if(okCoinDataList == null || okCoinDataList.size() == 0) {
			return;
		}
		OkCoinData data = okCoinDataList.get(0);
//		if(! data.getChannel().equals("ok_sub_spot_eth_depth")) {
//			return;
//		}
		if(okCoinDataList.get(0).getData() instanceof JSONObject) {
			OkcoinDepth depth = ((JSONObject) okCoinDataList.get(0).getData()).toJavaObject(OkcoinDepth.class);
			ListingFullInfo listingFullInfo = convert(data.getChannel(), depth);
			listingInfoMonitor.tell(listingFullInfo, ActorRef.noSender());
			//TODO send to ok coin consumer
		}
	}

	private ListingFullInfo convert(String channel, OkcoinDepth okcoinDepth) {
		if(okcoinDepth == null ||
				(okcoinDepth.getAsks() == null && okcoinDepth.getBids() == null) ) {
			return null;
		}
		ListingFullInfo listingFullInfo = new ListingFullInfo();
		listingFullInfo.setTradePlatform(TradePlatform.OKEX);
		listingFullInfo.setTradeType(this.tradeType);
		if(! this.tradeType.equals(TradeType.COIN_COIN)) {
			throw new IllegalArgumentException("Trade type " + this.tradeType + "not supported");
		}
		Pair<String> pair = parseCoinSource(channel);
		listingFullInfo.setSourceCoinType(CoinType.valueOf(pair.get_1().toUpperCase()));
		listingFullInfo.setTargetCoinType(CoinType.valueOf(pair.get_2().toUpperCase()));
		listingFullInfo.setBuyDepth(convert(okcoinDepth.getAsks()));
		listingFullInfo.setSellDepth(convert(okcoinDepth.getBids()));
		listingFullInfo.setTimestamp(okcoinDepth.getTimestamp());
		return listingFullInfo;
	}

	private ListingDepth convert(List<List<String>> depths) {
		if(depths == null) {
			return null;
		}
		ListingDepth listingDepth = new ListingDepth();
		for (List<String> one : depths) {
			ListingDepth.DepthInfo depthInfo = listingDepth.new DepthInfo();
			depthInfo.setPrice(Double.parseDouble(one.get(0)));
			depthInfo.setAmount(Double.parseDouble(one.get(1)));
			listingDepth.getDepthInfoMap().put(depthInfo.getPrice(), depthInfo);
		}
		return listingDepth;
	}

	/**
	 *
	 * @param channel ok_sub_spot_bch_btc_depth
	 * @return Pair<bch, btc></>
	 */
	public Pair<String> parseCoinSource(String channel) {
		Pair<String> pair = new Pair<>();
		int index1 = channel.indexOf("ok_sub_spot_") + "ok_sub_spot_".length();
		int index2 = channel.indexOf("_", index1 + 1);
		int index3 = channel.indexOf("_", index2 + 1);
		String source = channel.substring(index1, index2);
		String target = channel.substring(index2 + 1, index3);
		pair.set_1(source);
		pair.set_2(target);

		return pair;
	}
}
