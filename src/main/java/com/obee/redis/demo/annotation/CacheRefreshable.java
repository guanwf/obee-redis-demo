package com.obee.redis.demo.annotation;

/**
 * @description:
 * @author: Guanwf
 * @date: 2026/2/15 15:17
 *
 *  * 缓存刷新契约接口
 *  * 让 DTO 支持控制是否强制刷新缓存
 *
 */
public interface CacheRefreshable {
    /**
     * 是否跳过读取缓存（强制刷新）
     * @return true: 跳过 Redis 读取，查 DB，并回写 Redis
     *         false: 正常走缓存逻辑
     */
    boolean isSkipCache();
}
