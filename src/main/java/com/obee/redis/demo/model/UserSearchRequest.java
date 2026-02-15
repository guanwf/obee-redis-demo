package com.obee.redis.demo.model;

import com.obee.redis.demo.annotation.CacheRefreshable;
import lombok.Data;

/**
 * @description:
 * @author: Guanwf
 * @date: 2026/2/15 15:18
 */
@Data
public class UserSearchRequest implements CacheRefreshable {
    private String username;
    private String city;

    // 控制参数
    private boolean forceRefresh = false;

    @Override
    public boolean isSkipCache() {
        return this.forceRefresh;
    }
}
