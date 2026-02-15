package com.obee.redis.demo.service;

import com.obee.redis.demo.model.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * @description:
 * @author: Guanwf
 * @date: 2026/2/14 20:38
 */
@Service
@Slf4j
public class UserService {

    @Autowired
    private RedisService redisService;

    public UserDTO getUser(String userId) {
        String cacheKey = "user:profile:" + userId;

        // 1. 尝试从缓存获取
        Optional<UserDTO> cache = redisService.get(cacheKey, UserDTO.class);
        if (cache.isPresent()) {
            return cache.get();
        }

        // 2. 缓存未命中，查库
        UserDTO user = null;///userMapper.findById(userId);

        // 3. 写入缓存 (1小时过期)
        if (user != null) {
            redisService.set(cacheKey, user, Duration.ofHours(1));
        }

        // 4. 原子记录访问次数 (使用 StringRedisTemplate 内部实现)
        redisService.increment("user:stats:view_count:" + userId, 1);

        return user;
    }


    // unless="#result == null": 如果数据库没查到，不缓存（注意：这可能导致缓存穿透，后面会讲解决方案）
    @Cacheable(value = "users", key = "#id", unless = "#result == null")
    public User getUserById(Long id) {
        log.info("查询数据库: {}", id);
        //return userMapper.selectById(id);
        return null;
    }

    // 更新数据时，务必删除缓存！
    @CacheEvict(value = "users", key = "#user.id")
    public void updateUser(User user) {
//        userMapper.updateById(user);
    }


}
