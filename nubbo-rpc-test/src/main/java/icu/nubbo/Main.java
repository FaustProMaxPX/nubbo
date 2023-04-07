package icu.nubbo;

import icu.nubbo.constant.zookeeper.ZKConstant;
import icu.nubbo.protocol.NubboProtocol;
import icu.nubbo.protocol.ServiceInfo;
import icu.nubbo.zookeeper.CuratorClient;

import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {
//        NubboContainer.run(Main.class);
//        NubboContainer container = NubboContainer.getContainer();
//        ZookeeperRegistry bean = container.getBean(ZookeeperRegistry.class);
//        if (bean != null) {
//            System.out.println("ok");
//            bean.registerService("localhost", 2181, List.of(new ServiceInfo("test", "")));
//            System.in.read();
//        }
        ServiceInfo serviceInfo = new ServiceInfo("123", "123");
        CuratorClient curatorClient = new CuratorClient("localhost:2181");
        NubboProtocol protocol = new NubboProtocol("localhost", 8080, List.of(serviceInfo));
        String pathData = curatorClient.createPathData(ZKConstant.ZK_PROVIDER_PATH + "/test", protocol.toJson().getBytes());
        byte[] data = curatorClient.getData(pathData);
        NubboProtocol protocol1 = NubboProtocol.fromJson(new String(data));
        System.out.println(protocol1.toJson());
        NubboClient client = new NubboClient("localhost:2181");

        while (true) {}
    }
}
