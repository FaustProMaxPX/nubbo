package icu.nubbo.codec;

import icu.nubbo.serializer.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class NubboDecoder extends ByteToMessageDecoder {

    private static final Logger log = LoggerFactory.getLogger(NubboEncoder.class);

    private final Class<?> genericClass;

    private final Serializer serializer;

    public NubboDecoder(Class<?> genericClass, Serializer serializer) {
        this.genericClass = genericClass;
        this.serializer = serializer;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 如果可读字节数小于4，代表没有长度信息，无法解析
        if (in.readableBytes() < 4) {
            return;
        }
        in.markReaderIndex();
        int length = in.readInt();
        // 如果可读字节数小于实际长度，表明这是个半包，重置读取进度，等待下次读取
        if (in.readableBytes() < length) {
            in.resetReaderIndex();
            return;
        }
        byte[] data = new byte[length];
        in.readBytes(data);
        try {
            Object o = serializer.deserializer(data, genericClass);
            out.add(o);
        } catch (Exception e) {
            log.error("decode failed, exception: " + e);
        }
    }
}
