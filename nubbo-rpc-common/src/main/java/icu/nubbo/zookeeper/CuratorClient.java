package icu.nubbo.zookeeper;

import icu.nubbo.constant.zookeeper.ZKConstant;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

import java.util.List;

public class CuratorClient {

    private final CuratorFramework curatorFramework;

    public CuratorClient(String connectString, String namespace, int sessionTimeout, int connectTimeout) {
        curatorFramework = CuratorFrameworkFactory.builder()
                .connectString(connectString)
                .namespace(namespace)
                .sessionTimeoutMs(sessionTimeout)
                .connectionTimeoutMs(connectTimeout)
                .retryPolicy(new RetryNTimes(3, 10))
                .build();
        curatorFramework.start();
    }

    public CuratorClient(String connectString, String namespace) {
        this(connectString, namespace, ZKConstant.ZK_SESSION_TIMEOUT, ZKConstant.ZK_CONNECTION_TIMEOUT);
    }

    public CuratorClient(String connectString) {
        this(connectString, ZKConstant.ZK_NAMESPACE, ZKConstant.ZK_SESSION_TIMEOUT, ZKConstant.ZK_CONNECTION_TIMEOUT);
    }

    public CuratorFramework getCuratorFramework() {
        return curatorFramework;
    }

    public String createPathData(String path, byte[] data) throws Exception {
        return curatorFramework.create()
                .creatingParentsIfNeeded()
                .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                .forPath(path, data);
    }

    public void deletePath(String path) throws Exception {
        curatorFramework.delete()
                .deletingChildrenIfNeeded()
                .forPath(path);
    }

    public Stat updatePathData(String path, byte[] data) throws Exception {
        return curatorFramework.setData()
                .forPath(path, data);
    }

    public void addConnectionStateListener(ConnectionStateListener listener) {
        curatorFramework.getConnectionStateListenable().addListener(listener);
    }


    public List<String> getChildren(String path) throws Exception {
        return curatorFramework.getChildren().forPath(path);
    }

    public byte[] getData(String path) throws Exception {
        return curatorFramework.getData().forPath(path);
    }

    public void watchPathChildrenNode(String path, PathChildrenCacheListener listener) throws Exception {
        PathChildrenCache cache = new PathChildrenCache(curatorFramework, path, true);
        // 以同步的方式初始化缓存
        cache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
        cache.getListenable().addListener(listener);
    }

    public void stop() {
        curatorFramework.close();
    }
}
