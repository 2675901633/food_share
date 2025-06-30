package cn.kmbeast.aop;

import java.lang.annotation.*;

/**
 * 分类缓存注解，标记需要缓存的分类相关方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheableCategory {

    /**
     * 缓存过期时间（秒），默认1天
     */
    long expire() default 86400;
}