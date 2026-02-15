package com.obee.redis.demo.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * @description:
 * @author: Guanwf
 * @date: 2026/2/14 20:44
 */
@Target(ElementType.METHOD) // 作用在方法上
@Retention(RetentionPolicy.RUNTIME) // 运行时有效
@Documented
public @interface KaimingCache {

    /**
     * 缓存 Key，支持 SpEL 表达式
     * 例如：'user:' + #userId
     *
     * 支持 自定义函数: "'search:' + #hash(#req)"
     * 支持 环境变量: "${app.cache.prefix} + #id"
     */
    String key();

    /**
     * 过期时间，默认 60 秒
     */
    long timeout() default 60;

    /**
     * 【新增】支持引用配置文件的过期时间
     * 例如: "${app.cache.user.timeout:600}"
     * 如果设置了此值，优先使用此值覆盖 timeout()
     */
    String timeoutString() default "";

    /**
     * 时间单位，默认秒
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 是否启用随机过期时间（防止缓存雪崩）
     * 如果为 true，会在 timeout 基础上增加 0-20% 的随机时间
     */
    boolean random() default true;

}
