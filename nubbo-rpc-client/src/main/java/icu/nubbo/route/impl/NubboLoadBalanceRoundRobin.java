package icu.nubbo.route.impl;

import icu.nubbo.handler.NubboClientHandler;
import icu.nubbo.protocol.NubboProtocol;
import icu.nubbo.route.NubboLoadBalance;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class NubboLoadBalanceRoundRobin extends NubboLoadBalance {

    private AtomicInteger rounds = new AtomicInteger(0);

    @Override
    public NubboProtocol route(String serviceKey, Map<NubboProtocol, NubboClientHandler> connectedServerNodes) {
        Map<String, List<NubboProtocol>> serviceMap = getServiceMap(connectedServerNodes);
        List<NubboProtocol> protocolList = serviceMap.get(serviceKey);
        if (protocolList != null && !protocolList.isEmpty()) {
            return doRoute(protocolList);
        } else {
            throw new RuntimeException("无法与目标服务建立连接：" + serviceKey);
        }
    }

    private NubboProtocol doRoute(List<NubboProtocol> protocolList) {
        int size = protocolList.size();
        int index = (rounds.getAndAdd(1) + size) % size;
        return protocolList.get(index);
    }
}
