package cn.kmbeast.controller;

import cn.kmbeast.annotation.RateLimit;
import cn.kmbeast.annotation.RateLimitType;
import cn.kmbeast.service.impl.EnhancedRedisHealthService;
import cn.kmbeast.service.impl.MultiLevelCacheService;
import cn.kmbeast.service.impl.RateLimitService;
import cn.kmbeast.utils.ApiResult;
import cn.kmbeast.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 增强版Redis健康监控控制器
 */
@Slf4j
@RestController
@RequestMapping("/redis/health")
public class EnhancedRedisHealthController {

    @Autowired
    private EnhancedRedisHealthService redisHealthService;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private MultiLevelCacheService multiLevelCacheService;

    /**
     * 基础健康检查
     */
    @GetMapping("/check")
    public Result<Map<String, Object>> healthCheck() {
        Map<String, Object> healthInfo = redisHealthService.checkRedisHealth();

        if ("UP".equals(healthInfo.get("status"))) {
            return ApiResult.success("Redis连接正常", healthInfo);
        } else {
            return ApiResult.error("Redis连接异常", healthInfo);
        }
    }

    /**
     * 详细监控指标
     */
    @GetMapping("/metrics")
    @RateLimit(limit = 30, window = 60, type = RateLimitType.IP)
    public Result<Map<String, Object>> getDetailedMetrics() {
        Map<String, Object> metrics = redisHealthService.getDetailedHealthInfo();
        return ApiResult.success("Redis详细指标", metrics);
    }

    /**
     * 性能指标
     */
    @GetMapping("/performance")
    @RateLimit(limit = 20, window = 60, type = RateLimitType.IP)
    public Result<Map<String, Object>> getPerformanceMetrics() {
        Map<String, Object> healthInfo = redisHealthService.getDetailedHealthInfo();
        Map<String, Object> performance = (Map<String, Object>) healthInfo.get("performance");
        return ApiResult.success("Redis性能指标", performance);
    }

    /**
     * 缓存统计
     */
    @GetMapping("/cache-stats")
    @RateLimit(limit = 20, window = 60, type = RateLimitType.IP)
    public Result<Map<String, Object>> getCacheStats() {
        Map<String, Object> healthInfo = redisHealthService.getDetailedHealthInfo();
        Map<String, Object> cacheStats = (Map<String, Object>) healthInfo.get("cache");
        return ApiResult.success("缓存统计信息", cacheStats);
    }

    /**
     * 限流统计
     */
    @GetMapping("/rate-limit-stats")
    @RateLimit(limit = 10, window = 60, type = RateLimitType.IP)
    public Result<Map<String, Object>> getRateLimitStats() {
        Map<String, Object> stats = new HashMap<>();

        // 这里可以添加限流统计逻辑
        stats.put("message", "限流统计功能");
        stats.put("timestamp", System.currentTimeMillis());

        return ApiResult.success("限流统计信息", stats);
    }
} 