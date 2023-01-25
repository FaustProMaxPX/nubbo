package icu.nubbo.ioc.common;

import icu.nubbo.ioc.common.enums.InitWay;

import java.util.Objects;

/**
 * 表示初始化bean所需信息的类
 * */
public class BeanInitInfo {

    /** 需要初始化的类 */
    private Class<?> clazz;

    /** 初始化方式 */
    private InitWay way;

    /** Bean方法初始化需要的额外信息 */
    private MethodInitInfo methodInitInfo;

    public BeanInitInfo(Class<?> clazz, InitWay way) {
        this.clazz = clazz;
        this.way = way;
        methodInitInfo = null;
    }

    public BeanInitInfo(Class<?> clazz, InitWay way, MethodInitInfo methodInitInfo) {
        this.clazz = clazz;
        this.way = way;
        this.methodInitInfo = methodInitInfo;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    public InitWay getWay() {
        return way;
    }

    public void setWay(InitWay way) {
        this.way = way;
    }

    public MethodInitInfo getMethodInitInfo() {
        return methodInitInfo;
    }

    public void setMethodInitInfo(MethodInitInfo methodInitInfo) {
        this.methodInitInfo = methodInitInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BeanInitInfo that = (BeanInitInfo) o;

        if (!Objects.equals(clazz, that.clazz)) return false;
        if (way != that.way) return false;
        return Objects.equals(methodInitInfo, that.methodInitInfo);
    }

    @Override
    public int hashCode() {
        int result = clazz != null ? clazz.hashCode() : 0;
        result = 31 * result + (way != null ? way.hashCode() : 0);
        result = 31 * result + (methodInitInfo != null ? methodInitInfo.hashCode() : 0);
        return result;
    }
}
