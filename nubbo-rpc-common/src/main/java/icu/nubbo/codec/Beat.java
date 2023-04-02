package icu.nubbo.codec;

/*
* 心跳
* */
public final class Beat {

    public static final int BEAT_INTERNAL = 30;

    public static final int BEAT_TIMEOUT = 3 * BEAT_INTERNAL;

    public static final String BEAT_ID = "BEAT_PING_PONG";

    public static NubboRequest BEAN_PING;

    static {
        BEAN_PING = new NubboRequest() {};
        BEAN_PING.setRequestId(BEAT_ID);
    }
}
