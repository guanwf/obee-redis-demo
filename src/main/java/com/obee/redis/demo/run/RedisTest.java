package com.obee.redis.demo.run;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @description:
 * @author: Guanwf
 * @date: 2026/2/14 20:18
 */
@Slf4j
@SpringBootApplication
public class RedisTest implements CommandLineRunner {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public void run(String... args) {
        redisTemplate.opsForValue().set("test", "ok");
        log.info("Redis连接成功: " + redisTemplate.opsForValue().get("test"));
    }

}
