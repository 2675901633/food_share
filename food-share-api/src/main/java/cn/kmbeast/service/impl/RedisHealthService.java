package cn.kmbeast.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Redis健康检查服务
 */
@Slf4j
@Service
public class RedisHealthService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 检查Redis连接状态
     */
    public Map<String, Object> checkRedisHealth() {
        Map<String, Object> healthInfo = new HashMap<>();

        try {
            // 执行ping命令
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();
            healthInfo.put("status", "UP");
            healthInfo.put("ping", pong);

            // 获取Redis信息
            healthInfo.put("info", getRedisInfo());

        } catch (Exception e) {
            log.error("Redis健康检查失败: {}", e.getMessage(), e);
            healthInfo.put("status", "DOWN");
            healthInfo.put("error", e.getMessage());
        }

        return healthInfo;
    }

    /**
     * 获取Redis基本信息
     */
    private Map<String, Object> getRedisInfo() {
        Map<String, Object> info = new HashMap<>();
        try {
            // 获取连接信息
            info.put("connectionType", redisTemplate.getConnectionFactory().getClass().getSimpleName());

            // 测试基本操作
            String testKey = "health:check:" + System.currentTimeMillis();
            redisTemplate.opsForValue().set(testKey, "test", 10);
            String testValue = (String) redisTemplate.opsForValue().get(testKey);
            redisTemplate.delete(testKey);

            info.put("basicOperation", "test".equals(testValue) ? "OK" : "FAILED");

        } catch (Exception e) {
            log.warn("获取Redis信息失败: {}", e.getMessage());
            info.put("error", e.getMessage());
        }

        return info;
    }
} 