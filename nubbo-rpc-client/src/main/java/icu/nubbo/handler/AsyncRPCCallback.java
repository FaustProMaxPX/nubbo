package icu.nubbo.handler;

public interface AsyncRPCCallback {

    void success(Object result);

    void fail(Exception e);
}
