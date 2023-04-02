package icu.nubbo.handler;

import icu.nubbo.codec.*;
import icu.nubbo.serializer.Serializer;
import icu.nubbo.serializer.kryo.KryoSerializer;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/*
* Nubbo客户端初始化
* */
public class NubboClientInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        Serializer serializer = new KryoSerializer();
        ChannelPipeline pipeline = socketChannel.pipeline();
//        添加一个心跳处理器
        pipeline.addLast(new IdleStateHandler(0, 0, Beat.BEAT_INTERNAL, TimeUnit.SECONDS));
        pipeline.addLast(new NubboEncoder(NubboRequest.class, serializer));
//        解决黏包问题
        pipeline.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0));
        pipeline.addLast(new NubboDecoder(NubboResponse.class, serializer));
        pipeline.addLast(new NubboClientHandler());
    }
}
