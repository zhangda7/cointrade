package com.spare.cointrade.realtime.okcoin;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.spare.cointrade.model.OkCoinData;
import com.spare.cointrade.model.OkcoinDepth;
import com.spare.cointrade.util.AkkaContext;
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

	private ActorSelection tradeJudge;

	public BuissnesWebSocketServiceImpl() {
		tradeJudge = AkkaContext.getSystem().actorSelection("akka://rootSystem/user/tradeJudge");
	}

	@Override
	public void onReceive(String msg) {
		if(msg.equals("{\"event\":\"pong\"}")) {
			return;
		}
//		log.info("WebSocket Client received message: " + msg);
		List<OkCoinData> okCoinDataList = JSON.parseObject(msg, type);
		if(okCoinDataList == null || okCoinDataList.size() == 0) {
			return;
		}
		OkCoinData data = okCoinDataList.get(0);
		if(! data.getChannel().equals("ok_sub_spot_btc_depth")) {
			return;
		}
		if(okCoinDataList.get(0).getData() instanceof JSONObject) {
			OkcoinDepth depth = ((JSONObject) okCoinDataList.get(0).getData()).toJavaObject(OkcoinDepth.class);
			tradeJudge.tell(depth, ActorRef.noSender());
			//TODO send to ok coin consumer
		}
	}
}
