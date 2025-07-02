package cn.kmbeast.annotation;

import java.lang.annotation.*;

/**
 * 限流注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流key，支持SpEL表达式
     */
    String key() default "";

    /**
     * 限流次数
     */
    int limit() default 10;

    /**
     * 时间窗口（秒）
     */
    int window() default 60;

    /**
     * 限流类型
     */
    RateLimitType type() default RateLimitType.USER;

    /**
     * 限流失败消息
     */
    String message() default "请求过于频繁，请稍后再试";
} 