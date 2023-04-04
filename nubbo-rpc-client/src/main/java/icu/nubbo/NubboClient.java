package icu.nubbo;

import icu.nubbo.connection.ConnectionManager;
import icu.nubbo.discovery.zookeeper.ZookeeperDiscovery;
import icu.nubbo.handler.NubboClientHandler;
import icu.nubbo.proxy.NubboRpcService;
import icu.nubbo.proxy.ObjectProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class NubboClient {

    public static final Logger log = LoggerFactory.getLogger(NubboClientHandler.class);

    private ZookeeperDiscovery discovery;

    private static final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(8, 16, 60L, TimeUnit.SECONDS,
            new LinkedBlockingDeque<>(1000), (r) -> {return new Thread(r, "nubbo-NubboClient-" + r.hashCode());}, new ThreadPoolExecutor.AbortPolicy());

    public NubboClient(String address) {
        discovery = new ZookeeperDiscovery(address);
    }

    @SuppressWarnings("unchecked")
    public static <T> T createService(Class<T> interfaceClass, String version) {
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class[]{interfaceClass},
                new ObjectProxy<T>(interfaceClass, version)
        );
    }

    public static <T> NubboRpcService createAsyncService(Class<T> interfaceClass, String version) {
        return new ObjectProxy<T>(interfaceClass, version);
    }

    public static void submit(Runnable task) {
        threadPoolExecutor.submit(task);
    }

    public void stop() {
        threadPoolExecutor.shutdown();
        discovery.stop();
        ConnectionManager.getInstance().stop();
    }
}
