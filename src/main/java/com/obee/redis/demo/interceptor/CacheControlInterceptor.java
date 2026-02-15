package com.obee.redis.demo.interceptor;

import com.obee.redis.demo.annotation.CacheControl;
import com.obee.redis.demo.service.CacheContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * @description:
 * @author: Guanwf
 * @date: 2026/2/14 22:11
 */
@Component
public class CacheControlInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 如果不是映射到Controller方法（比如是静态资源），直接跳过
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;

        // 1. 优先检查方法上的注解
        CacheControl methodAnnotation = handlerMethod.getMethodAnnotation(CacheControl.class);
        if (methodAnnotation != null) {
            CacheContext.setEnabled(methodAnnotation.enabled());
            return true;
        }

        // 2. 其次检查类（Controller）上的注解
        CacheControl classAnnotation = handlerMethod.getBeanType().getAnnotation(CacheControl.class);
        if (classAnnotation != null) {
            CacheContext.setEnabled(classAnnotation.enabled());
            return true;
        }

        // 3. 都没有，默认为 true (CacheContext 初始值就是 true，这里可以不操作)
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 【关键】请求结束必须清理 ThreadLocal，防止线程池复用导致污染
        CacheContext.clear();
    }

}
