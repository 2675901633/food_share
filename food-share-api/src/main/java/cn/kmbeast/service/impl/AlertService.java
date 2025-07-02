package cn.kmbeast.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 自动化告警服务
 */
@Slf4j
@Service
public class AlertService {

    @Autowired
    private EnhancedRedisHealthService redisHealthService;

    @Value("${app.alert.memory-threshold:80}")
    private double memoryThreshold;

    @Value("${app.alert.hit-rate-threshold:90}")
    private double hitRateThreshold;

    @Value("${app.alert.slow-log-threshold:10}")
    private long slowLogThreshold;

    /**
     * 定时检查告警条件
     */
    @Scheduled(fixedRate = 60000) // 每分钟检查一次
    public void checkAlerts() {
        try {
            Map<String, Object> healthInfo = redisHealthService.getDetailedHealthInfo();

            // 检查Redis连接状态
            checkRedisConnection(healthInfo);

            // 检查内存使用率
            checkMemoryUsage(healthInfo);

            // 检查缓存命中率
            checkHitRate(healthInfo);

            // 检查慢查询
            checkSlowQueries(healthInfo);

        } catch (Exception e) {
            log.error("告警检查失败: {}", e.getMessage(), e);
            sendAlert("告警系统异常", "告警检查过程中发生异常: " + e.getMessage(), AlertLevel.HIGH);
        }
    }

    /**
     * 检查Redis连接状态
     */
    private void checkRedisConnection(Map<String, Object> healthInfo) {
        String status = (String) healthInfo.get("status");
        if (!"UP".equals(status)) {
            sendAlert("Redis连接异常", "Redis服务不可用", AlertLevel.CRITICAL);
        }
    }

    /**
     * 检查内存使用率
     */
    private void checkMemoryUsage(Map<String, Object> healthInfo) {
        try {
            Map<String, Object> memory = (Map<String, Object>) healthInfo.get("memory");
            if (memory != null) {
                String fragRatioStr = (String) memory.get("memFragmentationRatio");
                if (fragRatioStr != null) {
                    double fragRatio = Double.parseDouble(fragRatioStr);
                    if (fragRatio > 1.5) {
                        sendAlert("Redis内存碎片率过高",
                                "当前内存碎片率: " + fragRatio, AlertLevel.MEDIUM);
                    }
                }
            }
        } catch (Exception e) {
            log.error("检查内存使用率失败: {}", e.getMessage());
        }
    }

    /**
     * 检查缓存命中率
     */
    private void checkHitRate(Map<String, Object> healthInfo) {
        try {
            Map<String, Object> performance = (Map<String, Object>) healthInfo.get("performance");
            if (performance != null) {
                String hitRateStr = (String) performance.get("hitRate");
                if (hitRateStr != null) {
                    double hitRate = Double.parseDouble(hitRateStr.replace("%", ""));
                    if (hitRate < hitRateThreshold) {
                        sendAlert("Redis缓存命中率过低",
                                "当前命中率: " + hitRateStr + ", 阈值: " + hitRateThreshold + "%",
                                AlertLevel.MEDIUM);
                    }
                }
            }
        } catch (Exception e) {
            log.error("检查缓存命中率失败: {}", e.getMessage());
        }
    }

    /**
     * 检查慢查询
     */
    private void checkSlowQueries(Map<String, Object> healthInfo) {
        try {
            Map<String, Object> slowlog = (Map<String, Object>) healthInfo.get("slowlog");
            if (slowlog != null) {
                Long slowLogCount = (Long) slowlog.get("slowLogCount");
                if (slowLogCount != null && slowLogCount > slowLogThreshold) {
                    sendAlert("Redis慢查询过多",
                            "当前慢查询数量: " + slowLogCount + ", 阈值: " + slowLogThreshold,
                            AlertLevel.MEDIUM);
                }
            }
        } catch (Exception e) {
            log.error("检查慢查询失败: {}", e.getMessage());
        }
    }

    /**
     * 发送告警
     */
    private void sendAlert(String title, String message, AlertLevel level) {
        log.warn("Redis告警 [{}] - {}: {}", level, title, message);

        // 这里可以集成实际的告警通知方式：
        // 1. 邮件通知
        // 2. 短信通知
        // 3. 钉钉/企业微信通知
        // 4. 监控系统API调用

        // 示例：记录到告警日志
        recordAlert(title, message, level);
    }

    /**
     * 记录告警信息
     */
    private void recordAlert(String title, String message, AlertLevel level) {
        // 可以存储到数据库或发送到监控系统
        log.info("告警记录 - 标题: {}, 内容: {}, 级别: {}, 时间: {}",
                title, message, level, System.currentTimeMillis());
    }

    /**
     * 告警级别枚举
     */
    public enum AlertLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
} 