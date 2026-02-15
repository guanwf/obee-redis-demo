package com.obee.redis.demo.service;

import com.obee.redis.demo.annotation.KaimingCache;
import com.obee.redis.demo.model.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @description:
 * @author: Guanwf
 * @date: 2026/2/14 21:40
 */
@Service
@Slf4j
public class DemoService {

    /**
     * 场景1：普通查询
     * Key 示例: "user:info:1001"
     * 过期时间: 30分钟 + 随机抖动
     */
    @KaimingCache(key = "'user:info:' + #id", timeout = 30, timeUnit = TimeUnit.MINUTES)
    public UserDTO getUserById(Long id) {
        log.info("Querying DB for user id: {}", id);
//        return userMapper.selectById(id);
        UserDTO userDTO=new UserDTO();
        userDTO.setId(id);
        userDTO.setName(id.toString());
        return userDTO;
    }

    /**
     * 场景2：对象作为参数
     * Key 示例: "user:search:shanghai:male"
     */
//    @KaimingCache(key = "'user:search:' + #query.city + ':' + #query.gender", timeout = 5)
//    public List<UserDTO> searchUsers(UserSearchQuery query) {
//        return userMapper.search(query);
//    }


    // 1. key 会先解析 ${app.cache.prefix} -> "prod:v1:"
    // 2. 然后解析 SpEL -> "prod:v1:user:1001"
    // 3. timeoutString 解析 -> 600秒
    @KaimingCache(
            key = "'${app.cache.prefix}user:' + #id",
            timeoutString = "${app.cache.search.ttl:300}"
    )
    public UserDTO getUser(Long id) {
        //return userMapper.selectById(id);
        log.info("Querying DB for user id: {}", id);
//        return userMapper.selectById(id);
        UserDTO userDTO=new UserDTO();
        userDTO.setId(id);
        userDTO.setName(id.toString());
        return userDTO;
    }

}
