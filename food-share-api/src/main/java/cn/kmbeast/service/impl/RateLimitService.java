package cn.kmbeast.service.impl;

import cn.kmbeast.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 通用限流服务
 * 基于Redis滑动窗口算法实现
 */
@Slf4j
@Service
public class RateLimitService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 滑动窗口限流Lua脚本
     */
    private static final String RATE_LIMIT_LUA_SCRIPT =
        "local key = KEYS[1] " +
        "local window = tonumber(ARGV[1]) " +
        "local limit = tonumber(ARGV[2]) " +
        "local now = tonumber(ARGV[3]) " +
        "local windowStart = now - window * 1000 " +

        "redis.call('zremrangebyscore', key, 0, windowStart) " +
        "local current = redis.call('zcard', key) " +

        "if current < limit then " +
        "  redis.call('zadd', key, now, now) " +
        "  redis.call('expire', key, window) " +
        "  return {1, limit - current - 1} " +
        "else " +
        "  return {0, 0} " +
        "end";

    /**
     * 检查是否允许访问
     * @param key 限流键
     * @param limit 限制次数
     * @param windowSeconds 时间窗口（秒）
     * @return 是否允许访问
     */
    public boolean isAllowed(String key, int limit, int windowSeconds) {
        try {
            long now = System.currentTimeMillis();

            DefaultRedisScript<java.util.List> script = new DefaultRedisScript<>();
            script.setScriptText(RATE_LIMIT_LUA_SCRIPT);
            script.setResultType(java.util.List.class);

            java.util.List<Long> result = redisTemplate.execute(script,
                Collections.singletonList(key),
                String.valueOf(windowSeconds),
                String.valueOf(limit),
                String.valueOf(now));

            if (result != null && result.size() >= 2) {
                Long allowed = result.get(0);
                Long remaining = result.get(1);

                log.debug("限流检查 - key: {}, 允许: {}, 剩余: {}", key, allowed == 1, remaining);
                return allowed == 1;
            }

            return false;
        } catch (Exception e) {
            log.error("限流检查异常: {}", e.getMessage(), e);
            // 异常时允许访问，避免影响正常业务
            return true;
        }
    }

    /**
     * 获取剩余访问次数
     */
    public long getRemainingCount(String key, int limit, int windowSeconds) {
        try {
            long now = System.currentTimeMillis();
            long windowStart = now - windowSeconds * 1000L;

            // 清理过期数据
            redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

            // 获取当前计数
            Long current = redisTemplate.opsForZSet().zCard(key);
            return Math.max(0, limit - (current != null ? current : 0));

        } catch (Exception e) {
            log.error("获取剩余次数异常: {}", e.getMessage(), e);
            return limit;
        }
    }
} 