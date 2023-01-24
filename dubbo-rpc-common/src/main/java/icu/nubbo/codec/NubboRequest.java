package icu.nubbo.codec;

import java.io.Serializable;

/**
 * rpc请求
 * 包含请求方法，请求参数，请求参数类型
 * 请求实体类，版本
 * 给请求赋予id，避免重复请求
 * */
public class NubboRequest implements Serializable {

    private String requestId;

    private String className;

    private String methodName;

    private Class<?>[] parameterTypes;

    private Object[] parameters;

    private String version;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public static class RequestBuilder {
        private NubboRequest request;

        public RequestBuilder() {
            request = new NubboRequest();
        }

        public void requestId(String requestId) {
            request.setRequestId(requestId);
        }

        public void className(String className) {
            request.setClassName(className);
        }

        public void methodName(String methodName) {
            request.setMethodName(methodName);
        }

        public void parameterTypes(Class<?>[] parameterTypes) {
            request.setParameterTypes(parameterTypes);
        }

        public void parameters(Object[] parameters) {
            request.setParameters(parameters);
        }

        public void version(String version) {
            request.setVersion(version);
        }

        public NubboRequest build() {
            return request;
        }
    }
}
