package icu.nubbo.zookeeper;

import icu.nubbo.zookeeper.constant.Constant;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

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
        this(connectString, namespace, Constant.ZK_SESSION_TIMEOUT, Constant.ZK_CONNECTION_TIMEOUT);
    }

    public CuratorClient(String connectString) {
        this(connectString, Constant.ZK_NAMESPACE, Constant.ZK_SESSION_TIMEOUT, Constant.ZK_CONNECTION_TIMEOUT);
    }

    public CuratorFramework getCuratorFramework() {
        return curatorFramework;
    }

    public String createPathData(String path, byte[] data) throws Exception {
        return curatorFramework.create()
                .creatingParentsIfNeeded()
                .withMode(CreateMode.EPHEMERAL)
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
}
