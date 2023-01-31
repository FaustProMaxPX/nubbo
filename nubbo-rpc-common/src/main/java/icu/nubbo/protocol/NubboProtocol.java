package icu.nubbo.protocol;

import com.alibaba.fastjson2.JSON;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * rpc服务协议
 * 包含服务方的主机名，端口号以及服务名列表
 * */
public class NubboProtocol implements Serializable {

    private String host;

    private Integer port;

    private List<ServiceInfo> serviceInfoList;

    public NubboProtocol(String host, Integer port, List<ServiceInfo> serviceInfoList) {
        this.host = host;
        this.port = port;
        this.serviceInfoList = serviceInfoList;
    }

    public String toJson() {
        return JSON.toJSONString(this);
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public List<ServiceInfo> getServiceInfoList() {
        return serviceInfoList;
    }

    public void setServiceInfoList(List<ServiceInfo> serviceInfoList) {
        this.serviceInfoList = serviceInfoList;
    }

    public static NubboProtocol fromJson(String json) {
        return JSON.parseObject(json, NubboProtocol.class);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NubboProtocol that = (NubboProtocol) o;
        return Objects.equals(host, that.host) && Objects.equals(port, that.port) && Objects.equals(serviceInfoList, that.serviceInfoList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, serviceInfoList.hashCode());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("host: ").append(host).append('\n');
        builder.append("port: ").append(port).append('\n');
        builder.append("serviceInfoList: ").append('\n');
        for (ServiceInfo serviceInfo : serviceInfoList) {
            builder.append('\t').append(serviceInfo).append('\n');
        }
        return builder.toString();
    }
}
