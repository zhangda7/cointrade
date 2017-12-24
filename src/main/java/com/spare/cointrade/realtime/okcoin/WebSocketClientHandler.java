package com.spare.cointrade.realtime.okcoin;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import com.spare.cointrade.realtime.okcoin.model.OkcoinDepth;
import com.spare.cointrade.util.AkkaContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {

    private static Logger logger = LoggerFactory.getLogger(WebSocketClientHandler.class);

    private final WebSocketClientHandshaker handshaker;
    private ChannelPromise handshakeFuture;
    private MoniterTask moniter;
    private WebSocketService service ;

    private ActorSelection tradeJudge;

    public WebSocketClientHandler(WebSocketClientHandshaker handshaker,WebSocketService service,MoniterTask moniter) {
        this.handshaker = handshaker;
        this.service = service;
        this.moniter = moniter;
        this.tradeJudge = AkkaContext.getSystem().actorSelection("akka://rootSystem/user/tradeJudge");
    }

    private void sendClearMsg() {
        OkcoinDepth depth = new OkcoinDepth();
        depth.setClear(true);
        this.tradeJudge.tell(depth, ActorRef.noSender());
    }

    public ChannelFuture handshakeFuture() {
        return handshakeFuture;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        handshaker.handshake(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.info("WebSocket Client disconnected!");
        sendClearMsg();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel ch = ctx.channel();
        moniter.updateTime();
        if (!handshaker.isHandshakeComplete()) {
            handshaker.finishHandshake(ch, (FullHttpResponse) msg);
            logger.info("WebSocket Client connected!");
            sendClearMsg();
            handshakeFuture.setSuccess();
            return;
        }

        if (msg instanceof FullHttpResponse) {
            FullHttpResponse response = (FullHttpResponse) msg;
            throw new IllegalStateException(
                    "Unexpected FullHttpResponse (getStatus=" + response.getStatus() +
                            ", content=" + response.content().toString(CharsetUtil.UTF_8) + ')');
        }

        WebSocketFrame frame = (WebSocketFrame) msg;
        if (frame instanceof TextWebSocketFrame) {
        	 TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
        	 service.onReceive(textFrame.text());
        } else if (frame instanceof BinaryWebSocketFrame) {
        	BinaryWebSocketFrame binaryFrame=(BinaryWebSocketFrame)frame;
        	service.onReceive(decodeByteBuff(binaryFrame.content()));
        }else if (frame instanceof PongWebSocketFrame) {
            logger.info("WebSocket Client received pong");
        } else if (frame instanceof CloseWebSocketFrame) {
            logger.info("WebSocket Client received closing");
            ch.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        if (!handshakeFuture.isDone()) {
            handshakeFuture.setFailure(cause);
        }
        ctx.close();
    }
    public  String decodeByteBuff(ByteBuf buf) throws IOException, DataFormatException {
    
    	   byte[] temp = new byte[buf.readableBytes()];
    	   ByteBufInputStream bis = new ByteBufInputStream(buf);
		   bis.read(temp);
		   bis.close();
		   Inflater decompresser = new Inflater(true);
		   decompresser.setInput(temp, 0, temp.length);
		   StringBuilder sb = new StringBuilder();
		   byte[] result = new byte[1024];
		   while (!decompresser.finished()) {
				int resultLength = decompresser.inflate(result);
				sb.append(new String(result, 0, resultLength, "UTF-8"));
		   }
		   decompresser.end();
           return sb.toString();
	}
}
