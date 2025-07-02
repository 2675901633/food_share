package cn.kmbeast.annotation;

/**
 * 限流类型枚举
 */
public enum RateLimitType {
    /**
     * 按用户限流
     */
    USER,

    /**
     * 按IP限流
     */
    IP,

    /**
     * 全局限流
     */
    GLOBAL,

    /**
     * 自定义限流
     */
    CUSTOM
} 