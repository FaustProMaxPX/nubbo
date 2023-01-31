package icu.nubbo.discovery.zookeeper;

import icu.nubbo.constant.zookeeper.ZKConstant;
import icu.nubbo.discovery.Discovery;
import icu.nubbo.protocol.NubboProtocol;
import icu.nubbo.zookeeper.CuratorClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务发现，zookeeper实现
 * */
public class ZookeeperDiscovery implements Discovery {

    private static final Logger log = LoggerFactory.getLogger(ZookeeperDiscovery.class);

    private CuratorClient curatorClient;

    public ZookeeperDiscovery(String connectString) {
        curatorClient = new CuratorClient(connectString);
        discoverService();
    }

    // 从zookeeper获取服务
    private void discoverService() {
        log.info("初始化服务列表");
        try {
            getServiceAndUpdate();
            // 为服务注册节点添加监听事件，监听节点的更新
            curatorClient.watchPathChildrenNode(ZKConstant.ZK_PROVIDER_PATH, new PathChildrenCacheListener() {
                @Override
                public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
                    PathChildrenCacheEvent.Type type = pathChildrenCacheEvent.getType();
                    ChildData data = pathChildrenCacheEvent.getData();
                    switch (type) {
                        case CONNECTION_RECONNECTED -> {
                            log.info("重连至zk，重新注册节点");
                            getServiceAndUpdate();
                        }
                        case CHILD_ADDED, CHILD_UPDATED, CHILD_REMOVED -> {
                            getServiceAndUpdate(data, type);
                        }
                    }
                }
            });
        } catch (Exception e) {
            log.error("初始化服务列表失败");
        }

    }

    private void getServiceAndUpdate(ChildData data, PathChildrenCacheEvent.Type type) {
        String path = data.getPath();
        byte[] bytes = data.getData();
        NubboProtocol protocol = NubboProtocol.fromJson(new String(bytes));
        updateConnectedServer(protocol, type);
    }

    private void updateConnectedServer(NubboProtocol protocol, PathChildrenCacheEvent.Type type) {
        // TODO：更新连接管理器中的相关数据
    }

    private void getServiceAndUpdate() throws Exception {
        List<String> nodes = curatorClient.getChildren(ZKConstant.ZK_PROVIDER_PATH);
        List<NubboProtocol> dataList = new ArrayList<>();
        // 获取子节点中存储的元数据，并缓存起来
        for (String node : nodes) {
            byte[] data = curatorClient.getData(ZKConstant.ZK_PROVIDER_PATH + "/" + node);
            String json = new String(data);
            NubboProtocol protocol = NubboProtocol.fromJson(json);
            dataList.add(protocol);
            // 更新存储的服务元数据
            updateConnectedServer(dataList);
        }
    }

    private void updateConnectedServer(List<NubboProtocol> dataList) {
        // TODO: 更新存储的服务元数据
    }
}
