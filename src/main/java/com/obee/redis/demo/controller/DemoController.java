package com.obee.redis.demo.controller;

import com.obee.redis.demo.annotation.CacheControl;
import com.obee.redis.demo.model.UserDTO;
import com.obee.redis.demo.model.UserSearchRequest;
import com.obee.redis.demo.service.DemoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @description:
 * @author: Guanwf
 * @date: 2026/2/15 11:39
 */
@RestController
@Slf4j
@RequestMapping("/demo")
//@CacheControl(enabled = false)
public class DemoController {

    @Autowired
    DemoService demoService;


    @CacheControl(enabled = false)
    @GetMapping("/getUserId")
    public void getUserId() {
        log.info("test");
        demoService.getUserById(2L);
    }

    @PostMapping("/search")
    public List<UserDTO> search(@RequestBody UserSearchRequest request) {
        // Controller 只需要透传参数
        demoService.getUserById(3L);
        return null;
    }

    @CacheControl(enabled = true)
    @GetMapping("/getUser")
    public void getUser() {
        log.info("test");
        demoService.getUser(2L);
    }

}
