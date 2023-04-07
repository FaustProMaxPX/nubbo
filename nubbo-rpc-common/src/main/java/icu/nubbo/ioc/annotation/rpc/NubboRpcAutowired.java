package icu.nubbo.ioc.annotation.rpc;

import icu.nubbo.ioc.annotation.NubboAutowired;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@NubboAutowired
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NubboRpcAutowired {
    String version() default "";
}
