package icu.nubbo.proxy;

import icu.nubbo.handler.NubboFuture;

public interface NubboRpcService {

    NubboFuture call(String funcName, Object... args);
}
