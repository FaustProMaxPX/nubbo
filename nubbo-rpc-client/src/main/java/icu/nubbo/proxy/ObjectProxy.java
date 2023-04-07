package icu.nubbo.proxy;

import icu.nubbo.codec.NubboRequest;
import icu.nubbo.connection.ConnectionManager;
import icu.nubbo.handler.NubboClientHandler;
import icu.nubbo.handler.NubboFuture;
import icu.nubbo.utils.ServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;

public class ObjectProxy<T> implements InvocationHandler, NubboRpcService {

    private static final Logger log = LoggerFactory.getLogger(ObjectProxy.class);

    private Class<T> clazz;

    private String version;

    public ObjectProxy(Class<T> clazz, String version) {
        this.clazz = clazz;
        this.version = version;
    }

    @Override
    public NubboFuture call(String funcName, Object... args) {
        String serviceKey = ServiceUtil.makeServiceKey(this.clazz.getName(), this.version);
//        根据服务的key去获取对应的处理器
        NubboClientHandler handler = ConnectionManager.getInstance().chooseHandler(serviceKey);
        NubboRequest request = createRequest(clazz.getName(), funcName, args);
//        异步调用
        return handler.sendRequest(request);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        NubboRequest req = createRequest(method.getDeclaringClass().getName(), method.getName(), args);
        String serviceKey = ServiceUtil.makeServiceKey(method.getDeclaringClass().getName(), version);
        NubboClientHandler handler = ConnectionManager.getInstance().chooseHandler(serviceKey);
        NubboFuture f = handler.sendRequest(req);
//        同步调用
        return f.get();
    }

    private NubboRequest createRequest(String className, String methodName, Object[] args) {
        NubboRequest req = new NubboRequest();
        req.setRequestId(UUID.randomUUID().toString());
        req.setClassName(className);
        req.setMethodName(methodName);
        req.setParameters(args);
        req.setVersion(version);

        Class[] parameterTypes = new Class[args.length];

        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = args[i].getClass();
        }
        req.setParameterTypes(parameterTypes);
        return req;
    }
}
