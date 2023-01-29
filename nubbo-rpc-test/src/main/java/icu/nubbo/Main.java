package icu.nubbo;

import icu.nubbo.ioc.NubboContainer;
import icu.nubbo.protocol.ServiceInfo;
import icu.nubbo.zookeeper.ZookeeperRegistry;

import java.io.IOException;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException {
        NubboContainer.run(Main.class);
        NubboContainer container = NubboContainer.getContainer();
        ZookeeperRegistry bean = container.getBean(ZookeeperRegistry.class);
        if (bean != null) {
            System.out.println("ok");
            bean.registerService("localhost", 2181, List.of(new ServiceInfo("test", "")));
            System.in.read();
        }
    }
}
