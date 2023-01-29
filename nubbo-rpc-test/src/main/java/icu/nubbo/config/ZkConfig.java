package icu.nubbo.config;

import icu.nubbo.ioc.annotation.NubboBean;
import icu.nubbo.ioc.annotation.NubboConfiguration;
import icu.nubbo.zookeeper.CuratorClient;
import icu.nubbo.zookeeper.ZookeeperRegistry;

@NubboConfiguration
public class ZkConfig {

    @NubboBean
    public CuratorClient curatorClient() {
        return new CuratorClient("localhost:2181");
    }

    @NubboBean
    public ZookeeperRegistry zookeeperRegistry(CuratorClient curatorClient) {
        return new ZookeeperRegistry(curatorClient);
    }
}
