package icu.nubbo;

import icu.nubbo.connection.ConnectionManager;
import icu.nubbo.discovery.zookeeper.ZookeeperDiscovery;
import icu.nubbo.handler.NubboClientHandler;
import icu.nubbo.ioc.annotation.rpc.NubboRpcAutowired;
import icu.nubbo.proxy.NubboRpcService;
import icu.nubbo.proxy.ObjectProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class NubboClient implements ApplicationContextAware, DisposableBean {

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

    @Override
    public void destroy() throws Exception {
        this.stop();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
//        TODO: 尝试不使用spring容器来实现代理类的注入，思路：给自己实现的IOC容器留下钩子函数，当前Client继承这个类，并在钩子函数中实现对应的装配方法

        String[] beanNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            Field[] fields = bean.getClass().getDeclaredFields();
            try {
                for (Field field : fields) {
                    NubboRpcAutowired annotation = field.getAnnotation(NubboRpcAutowired.class);
                    if (annotation != null) {
                        String version = annotation.version();
                        field.setAccessible(true);
                        field.set(bean, createService(field.getType(), version));
                    }
                }
            } catch (IllegalAccessException e) {
                log.error(e.toString());
            }
        }
    }
}
