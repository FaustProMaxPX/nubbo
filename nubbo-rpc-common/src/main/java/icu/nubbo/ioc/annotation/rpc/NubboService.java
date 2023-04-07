package icu.nubbo.ioc.annotation.rpc;

import icu.nubbo.ioc.annotation.NubboComponent;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 将rpc服务类的代理注入Bean当中
 * */
@Retention(RetentionPolicy.RUNTIME)
@NubboComponent
@Documented
public @interface NubboService {
    String version() default "";
}
