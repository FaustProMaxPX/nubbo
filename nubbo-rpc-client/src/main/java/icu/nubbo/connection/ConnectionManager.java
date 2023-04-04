package icu.nubbo.connection;

import icu.nubbo.route.NubboLoadBalance;
import icu.nubbo.handler.NubboClientHandler;
import icu.nubbo.handler.NubboClientInitializer;
import icu.nubbo.protocol.NubboProtocol;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/* 连接管理器
* 饿汉单例
* */
public class ConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(ConnectionManager.class);

    private static final ConnectionManager instance = new ConnectionManager();

    private final EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

    private static final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(4, 8, 600L, TimeUnit.SECONDS, new LinkedBlockingDeque<>(1000));

    private Map<NubboProtocol, NubboClientHandler> connectedServerNodes = new ConcurrentHashMap<>();

    private final CopyOnWriteArraySet<NubboProtocol> protocolSet = new CopyOnWriteArraySet<>();

    private final ReentrantLock lock = new ReentrantLock();

//    用于替代Objects.wait()，实现线程间同步/通信的手段
    private final Condition condition = lock.newCondition();

    private long waitTimeout = 5000;

//    TODO: 初始化为默认实现类
    private NubboLoadBalance loadBalance;

//    保证连接器运行状态的可见性
    private volatile boolean isRunning = true;

    private ConnectionManager() {}

    public static ConnectionManager getInstance() {
        return instance;
    }

    public void updateConnectedServer(List<NubboProtocol> serviceList) {
        if (serviceList != null && !serviceList.isEmpty()) {
//            更新客户端本地的服务列表缓存
            Set<NubboProtocol> serviceSet = new HashSet<>(serviceList);
            for (NubboProtocol protocol : serviceSet) {
//                添加尚未连接的服务端
                if (!protocolSet.contains(protocol)) {
                    connectServerNode(protocol);
                }
            }
//            删除无用的连接
            for (NubboProtocol protocol : protocolSet) {
                if (!serviceSet.contains(protocol)) {
                    log.info("移除不再使用的协议" + protocol.toJson());
                    removeAndCloseHandler(protocol);
                }
            }
        } else {
//            没有可用的服务了
            log.warn("客户端没有可用的服务了");
            for (NubboProtocol protocol : protocolSet) {
                removeAndCloseHandler(protocol);
            }
        }
    }

    public void updateConnectedServer(NubboProtocol protocol, PathChildrenCacheEvent.Type type) {
        if (protocol == null) {
            return;
        }
        switch (type) {
            case CHILD_ADDED -> {
                if (!protocolSet.contains(protocol)) {
                    connectServerNode(protocol);
                }
            }
            case CHILD_UPDATED -> {
                removeAndCloseHandler(protocol);
                connectServerNode(protocol);
            }
            case CHILD_REMOVED -> {
                removeAndCloseHandler(protocol);
            }
            default -> throw new IllegalArgumentException("未知的服务注册修改类型");
        }
    }

    private void connectServerNode(NubboProtocol protocol) {
        if (protocol.getServiceInfoList() == null || protocol.getServiceInfoList().isEmpty()) {
            log.info("节点上没有可用的服务,host: {}, port: {}", protocol.getHost(), protocol.getPort());
            return;
        }
        protocolSet.add(protocol);
        log.info("连接到一个新的服务节点，host: {}, port: {}", protocol.getHost(), protocol.getPort());
        final InetSocketAddress remoteAddr = new InetSocketAddress(protocol.getHost(), protocol.getPort());
//        建立连接的事情交给线程池，所有的连接都会共用同一个Reactor，实现多路复用
//        Bootstrap仅仅是一个启动类，不是Reactor本身，eventLoopGroup才是
        threadPoolExecutor.submit(() -> {
            Bootstrap b = new Bootstrap();
            b.group(eventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new NubboClientInitializer());
            ChannelFuture f = b.connect(remoteAddr);
//            如果连接到服务器，就将处理器添加到缓存当中，然后唤醒正在等待处理器的请求线程
            f.addListener((ChannelFutureListener) channelFuture -> {
                if (channelFuture.isSuccess()) {
                    log.info("成功连接到远程服务，host: {}, port: {}", remoteAddr.getHostName(), remoteAddr.getPort());
                    NubboClientHandler handler = channelFuture.channel().pipeline().get(NubboClientHandler.class);
                    connectedServerNodes.put(protocol, handler);
                    handler.setRpcProtocol(protocol);
                    signalAvailableHandler();
                } else {
                    log.error("连接远程服务器失败，host: {}, port: {}", remoteAddr.getHostName(), remoteAddr.getPort());
                }
            });
        });
    }

    private void signalAvailableHandler() {
        lock.lock();
        try {
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private void removeAndCloseHandler(NubboProtocol protocol) {
        NubboClientHandler handler = connectedServerNodes.get(protocol);
        if (handler != null) {
            handler.close();
        }
        connectedServerNodes.remove(protocol);
        protocolSet.remove(protocol);
    }

    public void removeHandler(NubboProtocol rpcProtocol) {
        protocolSet.remove(rpcProtocol);
        connectedServerNodes.remove(rpcProtocol);
    }

    public void stop() {
        isRunning = false;
        for (NubboProtocol nubboProtocol : protocolSet) {
            removeAndCloseHandler(nubboProtocol);
        }
        signalAvailableHandler();
        threadPoolExecutor.shutdown();
        eventLoopGroup.shutdownGracefully();
    }

    private boolean waitingForHandler() throws InterruptedException {
        lock.lock();
        try {
            log.warn("正在等待可用的服务");
            return condition.await(this.waitTimeout, TimeUnit.MILLISECONDS);
        } finally {
            lock.unlock();
        }
    }

    public NubboClientHandler chooseHandler(String serviceKey) {
        int size = connectedServerNodes.values().size();
        while (isRunning && size == 0) {
            try {
                waitingForHandler();
                size = connectedServerNodes.values().size();
            } catch (InterruptedException e) {
                log.error("等待可用服务的过程被中断");
            }
        }
        NubboProtocol protocol = loadBalance.route(serviceKey, connectedServerNodes);
        NubboClientHandler handler = connectedServerNodes.get(protocol);
        if (handler != null) {
            return handler;
        } else {
            throw new RuntimeException("无法获取到可用的RPC连接");
        }
    }
}
