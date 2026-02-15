package com.obee.redis.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.obee.redis.demo.annotation.CacheRefreshable;
import com.obee.redis.demo.annotation.KaimingCache;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @description:
 * @author: Guanwf
 * @date: 2026/2/14 20:47
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class CacheAspect implements EmbeddedValueResolverAware {

    private final RedisService redisService;

    // SpEL 解析器，线程安全
    private final ExpressionParser parser = new SpelExpressionParser();

    private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    // Spring 的配置解析器（用于解析 ${...}）
    private StringValueResolver valueResolver;

    // 专门用于生成 Hash 的 ObjectMapper
    private ObjectMapper hashMapper;

    @PostConstruct
    public void init() {
        hashMapper = new ObjectMapper();
        // 【关键】配置 Map 按 Key 排序，保证 JSON 顺序一致
        hashMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        // 忽略未知的属性，防止报错
        hashMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.valueResolver = resolver;
    }

    @Around("@annotation(kaimingCache)")
    public Object around(ProceedingJoinPoint joinPoint, KaimingCache kaimingCache) throws Throwable {

        // ==================================================
        // 【新增逻辑】检查全局开关
        // ==================================================
        if (!CacheContext.isEnabled()) {
            log.info("Cache is disabled by Controller context. Skip Redis lookup.");
            // 直接执行目标方法（查库），不走缓存查询，也不回写（或者根据业务决定是否回写）
            return joinPoint.proceed();
        }

        String appName = valueResolver.resolveStringValue("${spring.application.name}");
        log.info("appName={}",appName);

        // 1. 计算过期时间 (支持环境变量)
        long ttl2 = kaimingCache.timeout();
        if (StringUtils.hasText(kaimingCache.timeoutString())) {
            // 解析 "${cache.timeout:100}"
            String resolvedString = valueResolver.resolveStringValue(kaimingCache.timeoutString());
            try {
                if (resolvedString != null) {
                    ttl2 = Long.parseLong(resolvedString);
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid timeout config: {}", resolvedString);
            }
        }

        // 2. 生成 Key (支持 环境变量 + SpEL + MD5)
        // 先解析环境变量： "${prefix}" -> "myapp:user:"
        String rawKeyPattern = valueResolver.resolveStringValue(kaimingCache.key());
        String redisKey = generateKey(rawKeyPattern, joinPoint);

        // 2. 【新增】检查方法参数，看是否有 Body 要求强制刷新
        boolean forceRefresh = false;
        Object[] args = joinPoint.getArgs();

        for (Object arg : args) {
            // Java 16+ / Java 21 模式匹配写法：直接转换
            if (arg instanceof CacheRefreshable refreshable && refreshable.isSkipCache()) {
                forceRefresh = true;
                log.info("Request Body requested Force Refresh. Skipping Redis read.");
                break; // 只要有一个参数要求刷新，就刷新
            }
        }

//        String redisKey = "";
        if (!forceRefresh) {

            // 1. 解析 SpEL 生成真实的 Key
            redisKey = generateKeyBySpEL(kaimingCache.key(), joinPoint);

            // 获取方法的返回类型
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Class<?> returnType = signature.getReturnType();

            // 2. 查询缓存
            // 注意：这里复用了之前封装的 get 方法
            // 3. 读取缓存逻辑
            // 只有在【不是强制刷新】的情况下，才读 Redis

            Optional<?> cachedValue = redisService.get(redisKey, returnType);
            if (cachedValue.isPresent()) {
                log.debug("Hit cache: {}", redisKey);
                return cachedValue.get();
            }
        }


        // 3. 缓存未命中，执行目标方法（查数据库）
        // 架构思考：这里可以加分布式锁（Double Check）防止击穿，但为了代码简洁，暂展示基础版
        Object result = joinPoint.proceed();

        // 4. 回写缓存
        if (result != null) {
            long ttl = kaimingCache.timeout();
            // 如果开启随机时间（防止雪崩）
            if (kaimingCache.random()) {
                // 增加 0 ~ 20% 的随机抖动
                long jitter = ThreadLocalRandom.current().nextLong(ttl / 5);
                ttl += jitter;
            }

            // 转换为 Duration
            Duration duration = Duration.of(ttl, kaimingCache.timeUnit().toChronoUnit());

            redisService.set(redisKey, result, duration);
            log.debug("Cache set: {} ttl: {}", redisKey, duration);
        } else {
            // 可选：缓存空对象防止穿透 (设置较短时间，例如 1 分钟)
            redisService.set(redisKey, new Object(), Duration.ofMinutes(1));
        }

        return result;
    }

    /**
     * 解析 SpEL 表达式
     *
     * @param spELString 注解上的 key 定义，如 "'user:' + #id"
     * @param joinPoint  切入点
     * @return 真实 Key
     */
    private String generateKeyBySpEL(String spELString, ProceedingJoinPoint joinPoint) {
        // 如果不是 SpEL 表达式（不包含#），直接返回
        if (!spELString.contains("#")) {
            return spELString;
        }

        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        Object[] args = joinPoint.getArgs();

        // 获取方法参数名
        String[] paramNames = nameDiscoverer.getParameterNames(method);

        // 构建上下文
        EvaluationContext context = new StandardEvaluationContext();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        // 解析
        Expression expression = parser.parseExpression(spELString);
        return expression.getValue(context, String.class);
    }

    /**
     * 生成 Key 的核心逻辑
     */
    private String generateKey(String spELString, ProceedingJoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        Object[] args = joinPoint.getArgs();
        String[] paramNames = nameDiscoverer.getParameterNames(method);

        // 构建 SpEL 上下文
        StandardEvaluationContext context = new StandardEvaluationContext();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        // 【关键】注册自定义函数 #hash()
        try {
            context.registerFunction("hash", CacheAspect.class.getDeclaredMethod("calculateHash", Object.class));
        } catch (NoSuchMethodException e) {
            log.error("Failed to register hash function", e);
        }

        return parser.parseExpression(spELString).getValue(context, String.class);
    }

    /**
     * 【自定义函数】计算对象的 MD5
     * 该方法必须是 static public，以便 SpEL 调用
     */
    public static String calculateHash(Object obj) {
        if (obj == null) return "null";
        // 简单类型直接返回
        if (obj instanceof String || obj instanceof Number || obj instanceof Boolean) {
            return String.valueOf(obj);
        }

        try {
            // 获取静态实例（注意：实际生产中建议把 HashMapper 提取为单例工具类，这里为了演示方便）
            // 由于 SpEL 调用的是静态方法，无法直接访问 Spring Bean 的实例字段
            // 这里我们临时 new 一个 或者 使用 Holder 模式。
            // 更好的方式是把 logic 放在一个单独的 Util 类里。
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);

            String json = mapper.writeValueAsString(obj);
            return DigestUtils.md5DigestAsHex(json.getBytes(StandardCharsets.UTF_8));
        } catch (JsonProcessingException e) {
            log.error("Hash calculation failed", e);
            return String.valueOf(obj.hashCode());
        }
    }

}
