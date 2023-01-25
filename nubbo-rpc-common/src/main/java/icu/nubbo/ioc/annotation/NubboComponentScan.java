package icu.nubbo.ioc.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface NubboComponentScan {
    String[] basePackages() default {""};
}
