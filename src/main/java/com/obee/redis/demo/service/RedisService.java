package com.obee.redis.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @description:
 * @author: Guanwf
 * @date: 2026/2/14 20:05
 */
@Slf4j
@Component
@RequiredArgsConstructor // 自动注入 final 字段
public class RedisService {

    // 用于存储对象（JSON序列化）
    private final RedisTemplate<String, Object> redisTemplate;

    // 架构决策：专门用于计数器、简单的String操作，避免JSON序列化带来的格式问题和性能损耗
    private final StringRedisTemplate stringRedisTemplate;

    // 用于对象转换（可选，用于复杂集合转换）
    private final ObjectMapper objectMapper;

    // =============================
    // 1. Key 基本操作 (Key Operations)
    // =============================

    /**
     * 判断 key 是否存在
     */
    public boolean hasKey(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Redis hasKey check failed: {}", key, e);
            return false;
        }
    }

    /**
     * 删除 key
     */
    public boolean delete(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.delete(key));
        } catch (Exception e) {
            log.error("Redis delete failed: {}", key, e);
            return false;
        }
    }

    /**
     * 批量删除 key
     */
    public long delete(Collection<String> keys) {
        try {
            Long count = redisTemplate.delete(keys);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("Redis batch delete failed", e);
            return 0;
        }
    }

    /**
     * 设置过期时间
     */
    public boolean expire(String key, Duration timeout) {
        try {
            return Boolean.TRUE.equals(redisTemplate.expire(key, timeout));
        } catch (Exception e) {
            log.error("Redis expire failed: {}", key, e);
            return false;
        }
    }

    // =============================
    // 2. 常规值操作 (String/Object Value)
    // =============================

    /**
     * 普通缓存放入
     */
    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            log.error("Redis set failed: {}", key, e);
        }
    }

    /**
     * 普通缓存放入并设置时间
     */
    public void set(String key, Object value, Duration timeout) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout);
        } catch (Exception e) {
            log.error("Redis set with timeout failed: {}", key, e);
        }
    }

    /**
     * 获取对象（支持泛型自动转换）
     * 使用 Optional 防止空指针，这是 Java 8+ 的最佳实践
     */
    public <T> Optional<T> get(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                // 如果 Redis 存的是 LinkedHashMap (Jackson默认行为) 或其他类型，这里利用 ObjectMapper 强转更加安全
                return Optional.of(objectMapper.convertValue(value, clazz));
            }
        } catch (Exception e) {
            log.error("Redis get failed: {}", key, e);
        }
        return Optional.empty();
    }

    // =============================
    // 3. 原子操作 (Atomic Operations)
    // 架构师建议：计数器必须使用 StringRedisTemplate，防止序列化干扰
    // =============================

    /**
     * 递增 (Incr)
     * @param key 键
     * @param delta 递增因子 (必须大于0)
     * @return 增加后的值
     */
    public long increment(String key, long delta) {
        if (delta < 0) {
            throw new IllegalArgumentException("Delta must be greater than 0");
        }
        try {
            Long val = stringRedisTemplate.opsForValue().increment(key, delta);
            return val != null ? val : 0;
        } catch (Exception e) {
            log.error("Redis increment failed: {}", key, e);
            return 0; // 这里的返回值需根据业务决定，有时抛出异常更好
        }
    }

    /**
     * 递减 (Decr)
     */
    public long decrement(String key, long delta) {
        if (delta < 0) {
            throw new IllegalArgumentException("Delta must be greater than 0");
        }
        try {
            Long val = stringRedisTemplate.opsForValue().increment(key, -delta);
            return val != null ? val : 0;
        } catch (Exception e) {
            log.error("Redis decrement failed: {}", key, e);
            return 0;
        }
    }

    // =============================
    // 4. Hash 操作 (Map结构)
    // =============================

    /**
     * HashGet
     */
    public <T> Optional<T> hGet(String key, String item, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForHash().get(key, item);
            if (value != null) {
                return Optional.of(objectMapper.convertValue(value, clazz));
            }
        } catch (Exception e) {
            log.error("Redis hGet failed: {} - {}", key, item, e);
        }
        return Optional.empty();
    }

    /**
     * HashSet
     */
    public void hSet(String key, String item, Object value) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
        } catch (Exception e) {
            log.error("Redis hSet failed: {} - {}", key, item, e);
        }
    }

    /**
     * HashDelete
     */
    public void hDel(String key, Object... item) {
        try {
            redisTemplate.opsForHash().delete(key, item);
        } catch (Exception e) {
            log.error("Redis hDel failed: {}", key, e);
        }
    }

    /**
     * 获取 Hash 中所有数据
     */
    public <T> Map<String, T> hGetAll(String key, Class<T> clazz) {
        try {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
            Map<String, T> result = new HashMap<>();
            entries.forEach((k, v) -> {
                result.put((String) k, objectMapper.convertValue(v, clazz));
            });
            return result;
        } catch (Exception e) {
            log.error("Redis hGetAll failed: {}", key, e);
            return Collections.emptyMap();
        }
    }

    // =============================
    // 5. Set 操作 (无序集合)
    // =============================

    /**
     * Set 添加
     */
    public long sSet(String key, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().add(key, values);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("Redis sSet failed: {}", key, e);
            return 0;
        }
    }

    /**
     * Set 获取所有元素
     */
    public <T> Set<T> sGet(String key, Class<T> clazz) {
        try {
            Set<Object> members = redisTemplate.opsForSet().members(key);
            if (members == null) return Collections.emptySet();
            return members.stream()
                    .map(v -> objectMapper.convertValue(v, clazz))
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("Redis sGet failed: {}", key, e);
            return Collections.emptySet();
        }
    }

    /**
     * Set 是否包含
     */
    public boolean sHasKey(String key, Object value) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value));
        } catch (Exception e) {
            log.error("Redis sHasKey failed: {}", key, e);
            return false;
        }
    }

    // =============================
    // 6. List 操作 (队列/列表)
    // =============================

    /**
     * List 右推 (入队)
     */
    public void lPush(String key, Object value) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
        } catch (Exception e) {
            log.error("Redis lPush failed: {}", key, e);
        }
    }

    /**
     * List 获取区间 (分页查询)
     * @param start 0
     * @param end -1 代表所有
     */
    public <T> List<T> lGet(String key, long start, long end, Class<T> clazz) {
        try {
            List<Object> list = redisTemplate.opsForList().range(key, start, end);
            if (list == null) return Collections.emptyList();
            return list.stream()
                    .map(v -> objectMapper.convertValue(v, clazz))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Redis lGet failed: {}", key, e);
            return Collections.emptyList();
        }
    }

}
