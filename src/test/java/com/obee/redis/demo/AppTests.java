package com.obee.redis.demo;

import com.obee.redis.demo.annotation.CacheControl;
import com.obee.redis.demo.service.DemoService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
class AppTests {

	@Autowired
	DemoService demoService;

	@Test
	@CacheControl(enabled = false)
	void contextLoads() {
		log.info("test");
		demoService.getUserById(2L);
	}

}
