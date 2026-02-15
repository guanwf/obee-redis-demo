package com.obee.redis.demo.config;

import com.obee.redis.demo.interceptor.CacheControlInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @description:
 * @author: Guanwf
 * @date: 2026/2/14 22:13
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private CacheControlInterceptor cacheControlInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 匹配所有路径
        registry.addInterceptor(cacheControlInterceptor).addPathPatterns("/**");
    }
}
