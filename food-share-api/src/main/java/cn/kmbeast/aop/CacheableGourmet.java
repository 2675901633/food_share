package cn.kmbeast.aop;

import java.lang.annotation.*;

/**
 * 美食缓存注解，标记需要缓存的美食相关方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheableGourmet {

    /**
     * 缓存类型
     */
    CacheType type() default CacheType.SINGLE;

    /**
     * 缓存过期时间（秒），默认2小时
     */
    long expire() default 7200;
}