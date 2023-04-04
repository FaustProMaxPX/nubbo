package icu.nubbo.codec;

import icu.nubbo.serializer.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings(value = "rawtypes")
public class NubboEncoder extends MessageToByteEncoder {

    private static final Logger log = LoggerFactory.getLogger(NubboEncoder.class);

    private final Class<?> genericClass;

    private final Serializer serializer;

    public NubboEncoder(Class<?> genericClass, Serializer serializer) {
        this.genericClass = genericClass;
        this.serializer = serializer;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object o, ByteBuf byteBuf) throws Exception {
        if (genericClass.isInstance(o)) {
            try {
                byte[] data = serializer.serialize(o);
                byteBuf.writeInt(data.length);
                byteBuf.writeBytes(data);
            } catch (Exception e) {
                log.error("encode failed, exception: " + e);
            }
        }
    }
}
