package icu.nubbo.registry;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import icu.nubbo.constant.NubboConstant;
import icu.nubbo.protocol.NubboProtocol;
import icu.nubbo.protocol.ServiceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 服务中心抽象类
 * 模板模式
 * */
// TODO: 这个类目前有点鸡肋
public abstract class AbstractRegistryService {

    private static final Logger log = LoggerFactory.getLogger(AbstractRegistryService.class);

    // 注册服务模板方法
    // 主要负责校验入参是否正确，参数组装，以及异常捕获
    public final void registerService(String host, int port, List<ServiceInfo> serviceList) {
        if (StrUtil.isBlank(host)) {
            log.debug("主机名为空");
            throw new IllegalArgumentException("主机名不可为空");
        }
        if (serviceList == null || serviceList.size() == 0) {
            log.debug("服务注册列表为空");
            return;
        }
        register(new NubboProtocol(host, port, serviceList));
    }

    public final void unregisterService(String host, int port, ServiceInfo service) {
        if (StrUtil.isBlank(host)) {
            log.debug("主机名为空");
            throw new IllegalArgumentException("主机名不可为空");
        }
        if (ObjectUtil.isEmpty(service)) {
            log.debug("指定删除的服务为空");
            return;
        }
        unregister(host, port, service.getServiceName() + NubboConstant.SERVICE_VERSION_SEP + service.getVersion());
    }

    /**
     * 服务注册
     * @param protocol 服务注册协议
     * */
    protected abstract void register(NubboProtocol protocol);

    /**
     * 取消注册
     * @param host 主机名
     * @param port 端口号
     * @param serviceName 服务相关信息
     * */
    protected abstract void unregister(String host, int port, String serviceName);
}
