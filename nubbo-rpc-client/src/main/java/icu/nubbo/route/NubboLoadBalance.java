package icu.nubbo.route;

import icu.nubbo.handler.NubboClientHandler;
import icu.nubbo.protocol.NubboProtocol;
import icu.nubbo.protocol.ServiceInfo;
import icu.nubbo.utils.ServiceUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class NubboLoadBalance {



    /**
     * 获取每个服务可用的实例map
     * */
    protected Map<String, List<NubboProtocol>> getServiceMap(Map<NubboProtocol, NubboClientHandler> connectedServerNodes) {
        Map<String, List<NubboProtocol>> serviceMap = new HashMap<>();
        if (connectedServerNodes != null && !connectedServerNodes.isEmpty()) {
            for (NubboProtocol protocol : connectedServerNodes.keySet()) {
                for (ServiceInfo serviceInfo : protocol.getServiceInfoList()) {
                    String serviceKey = ServiceUtil.makeServiceKey(serviceInfo.getServiceName(), serviceInfo.getVersion());
                    if (!serviceMap.containsKey(serviceKey)) {
                        serviceMap.put(serviceKey, new ArrayList<>());
                    }
                    serviceMap.get(serviceKey).add(protocol);
                }
            }
        }
        return serviceMap;
    }

    public abstract NubboProtocol route(String serviceKey, Map<NubboProtocol, NubboClientHandler> connectedServerNodes);
}
