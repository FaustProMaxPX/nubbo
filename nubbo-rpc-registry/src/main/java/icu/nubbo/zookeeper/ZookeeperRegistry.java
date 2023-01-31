package icu.nubbo.zookeeper;

import icu.nubbo.constant.zookeeper.ZKConstant;
import icu.nubbo.protocol.NubboProtocol;
import icu.nubbo.registry.AbstractRegistryService;
import org.apache.curator.framework.state.ConnectionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于Zookeeper的注册中心
 * 注册中心结构：
 * /nubbo/provider/data-*
 * */
public class ZookeeperRegistry extends AbstractRegistryService {

    private CuratorClient curatorClient;

    private List<String> pathList;

    private static final Logger log = LoggerFactory.getLogger(ZookeeperRegistry.class);

    public static final String DEFAULT_CONNECT_STRING = "localhost:2181";

    public ZookeeperRegistry() {
        curatorClient = new CuratorClient(DEFAULT_CONNECT_STRING);
        pathList = new ArrayList<>();
    }

    public ZookeeperRegistry(CuratorClient curatorClient) {
        this.curatorClient = curatorClient;
        pathList = new ArrayList<>();
    }

    @Override
    public void unregisterAllService() {
        for (String path : pathList) {
            try {
                curatorClient.deletePath(path);
            } catch (Exception e) {
                log.error("删除路径失败，错误参数: {}", path);
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void register(NubboProtocol protocol) {
        String proJson = protocol.toJson();
        byte[] data = proJson.getBytes();
        // TODO: 这种注册结构的问题在于无法快速修改指定服务，后面可以考虑更改结构
        String path = ZKConstant.ZK_DATA_PATH + "-" + proJson.hashCode();
        try {
            curatorClient.createPathData(path, data);
            pathList.add(path);
            curatorClient.addConnectionStateListener((curatorFramework, connectionState) -> {
                if (connectionState == ConnectionState.RECONNECTED) {
                    log.debug("重连zookeeper，尝试重新注册节点");
                    register(protocol);
                }
            });
        } catch (Exception e) {
            log.error("服务注册失败，注册参数：{}", protocol);
            throw new RuntimeException("服务注册失败");
        }
    }

    @Override
    protected void unregister(String host, int port, String serviceName) {

    }
}
