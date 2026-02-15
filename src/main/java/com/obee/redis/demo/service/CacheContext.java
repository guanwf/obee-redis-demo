package com.obee.redis.demo.service;

import lombok.extern.slf4j.Slf4j;

/**
 * 缓存上下文管理器
 * 用于在线程内传递“是否跳过缓存”的信号
 */

/**
 * @description:
 * @author: Guanwf
 * @date: 2026/2/14 22:00
 */
@Slf4j
public class CacheContext {
    // 默认是 true (启用缓存)
    private static final ThreadLocal<Boolean> ENABLE_CACHE = ThreadLocal.withInitial(() -> true);

    /**
     * 设置当前线程是否启用缓存
     */
    public static void setEnabled(boolean enabled) {
        ENABLE_CACHE.set(enabled);
    }

    /**
     * 获取当前状态
     */
    public static boolean isEnabled() {
        return ENABLE_CACHE.get();
    }

    /**
     * 清理上下文（防止内存泄漏，必须调用）
     */
    public static void clear() {
        ENABLE_CACHE.remove();
    }

}
