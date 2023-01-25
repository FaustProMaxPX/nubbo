package icu.nubbo.ioc.common;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/**
 * 通过Bean方法初始化需要的信息
 * */
public class MethodInitInfo {

    /** 调用者 */
    private Class<?> invoker;

    /** 调用的方法 */
    private Method method;

    /**
     * 方法参数类型列表
     * 所有参数都从IOC容器中寻找
     * */
    private Class<?>[] parameterTypes;

    public MethodInitInfo() {
    }

    public MethodInitInfo(Class<?> invoker, Method method, Class<?>[] parameterTypes) {
        this.invoker = invoker;
        this.method = method;
        this.parameterTypes = parameterTypes;
    }

    public Class<?> getInvoker() {
        return invoker;
    }

    public void setInvoker(Class<?> invoker) {
        this.invoker = invoker;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MethodInitInfo that = (MethodInitInfo) o;

        if (!Objects.equals(invoker, that.invoker)) return false;
        if (!Objects.equals(method, that.method)) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(parameterTypes, that.parameterTypes);
    }

    @Override
    public int hashCode() {
        int result = invoker != null ? invoker.hashCode() : 0;
        result = 31 * result + (method != null ? method.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(parameterTypes);
        return result;
    }
}
