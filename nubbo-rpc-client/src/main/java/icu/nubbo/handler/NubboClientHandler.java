package icu.nubbo.handler;

import icu.nubbo.codec.Beat;
import icu.nubbo.codec.NubboRequest;
import icu.nubbo.codec.NubboResponse;
import icu.nubbo.connection.ConnectionManager;
import icu.nubbo.protocol.NubboProtocol;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;

public class NubboClientHandler extends SimpleChannelInboundHandler<NubboResponse> {

    private static final Logger log = LoggerFactory.getLogger(NubboClientHandler.class);

    private ConcurrentHashMap<String, NubboFuture> pendingRPC = new ConcurrentHashMap<>();

    private volatile Channel channel;

    private SocketAddress remoteAddr;

    private NubboProtocol rpcProtocol;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, NubboResponse response) throws Exception {
        String requestId = response.getRequestId();
        NubboFuture f = pendingRPC.get(requestId);
        if (f != null) {
            pendingRPC.remove(requestId);
            f.done(response);
        } else {
            log.warn("无法获取请求id为 {} 的响应", requestId);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("客户端捕获到异常：{}", cause.getMessage());
        ctx.close();
    }

    public void close() {
        channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

    public NubboFuture sendRequest(NubboRequest request) {
        NubboFuture f = new NubboFuture(request);
        pendingRPC.put(request.getRequestId(), f);
        try {
            ChannelFuture channelFuture = channel.writeAndFlush(request).sync();
            if (!channelFuture.isSuccess()) {
                log.error("send request failed, request id: {}", request.getRequestId());
            }
        } catch (InterruptedException e) {
            log.error("send request exception: {}", e.getMessage());
        }
        return f;
    }

//    心跳超时事件处理，如果规定时间内没有收到服务端心跳，就重新发送心跳信息
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            sendRequest(Beat.BEAN_PING);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    public void setRpcProtocol(NubboProtocol rpcProtocol) {
        this.rpcProtocol = rpcProtocol;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        ConnectionManager.getInstance().removeHandler(rpcProtocol);
    }
}
