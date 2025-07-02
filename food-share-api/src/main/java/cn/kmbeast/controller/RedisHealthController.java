package cn.kmbeast.controller;

import cn.kmbeast.service.impl.RedisHealthService;
import cn.kmbeast.utils.ApiResult;
import cn.kmbeast.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Redis健康检查控制器
 */
@Slf4j
@RestController
@RequestMapping("/redis/health")
public class RedisHealthController {

    @Autowired
    private RedisHealthService redisHealthService;

    /**
     * Redis健康检查
     */
    @GetMapping("/check")
    public Result<Map<String, Object>> healthCheck() {
        try {
            Map<String, Object> healthInfo = redisHealthService.checkRedisHealth();

            if ("UP".equals(healthInfo.get("status"))) {
                return ApiResult.success("Redis连接正常", healthInfo);
            } else {
                return ApiResult.error("Redis连接异常", healthInfo);
            }

        } catch (Exception e) {
            log.error("Redis健康检查异常: {}", e.getMessage(), e);
            return ApiResult.error("健康检查异常: " + e.getMessage());
        }
    }
} 