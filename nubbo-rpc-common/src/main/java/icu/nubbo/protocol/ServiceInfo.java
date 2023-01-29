package icu.nubbo.protocol;

import java.io.Serializable;
import java.util.Objects;

public class ServiceInfo implements Serializable {

    private String serviceName;

    private String version;

    public ServiceInfo() {
    }

    public ServiceInfo(String serviceName, String version) {
        this.serviceName = serviceName;
        this.version = version;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServiceInfo that = (ServiceInfo) o;

        if (!Objects.equals(serviceName, that.serviceName)) return false;
        return Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceName, version);
    }
}
