package icu.nubbo.zookeeper.constant;

public interface Constant {
    // 注册中心默认根路径
    String ZK_NAMESPACE = "nubbo";
    // 会话超时默认值
    int ZK_SESSION_TIMEOUT = 5000;
    // 连接超时默认值
    int ZK_CONNECTION_TIMEOUT = 5000;
    // 注册中心根路径
    String ZK_PROVIDER_PATH = "/provider";
    // 服务数据路径
    String ZK_DATA_PATH = ZK_PROVIDER_PATH + "/data";
}
