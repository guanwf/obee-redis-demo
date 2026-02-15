package com.obee.redis.demo.annotation;

import java.lang.annotation.*;

/**
 * @description:
 * @author: Guanwf
 * @date: 2026/2/14 21:49
 */
@Target({ElementType.TYPE, ElementType.METHOD}) // 类和方法都能用
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheControl {
    /**
     * 是否启用缓存
     * false: 整个调用链禁用读取缓存（强制查库），但依然允许写入缓存（取决于具体业务，通常查库后会回写）
     * true: 正常使用缓存
     */
    boolean enabled() default true;

    /**
     * 是否只读不写？（扩展属性，可选）
     */
    boolean readOnly() default false;
}
