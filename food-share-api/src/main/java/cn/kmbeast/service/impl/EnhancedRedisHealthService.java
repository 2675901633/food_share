package cn.kmbeast.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 增强版Redis健康检查服务
 */
@Slf4j
@Service
public class EnhancedRedisHealthService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private MultiLevelCacheService multiLevelCacheService;

    /**
     * 增强版Redis健康检查
     */
    public Map<String, Object> getDetailedHealthInfo() {
        Map<String, Object> healthInfo = new HashMap<>();

        try {
            // 1. 基础连接检查
            healthInfo.put("connection", checkConnection());

            // 2. 性能指标
            healthInfo.put("performance", getPerformanceMetrics());

            // 3. 内存使用情况
            healthInfo.put("memory", getMemoryMetrics());

            // 4. 集群状态（如果是集群模式）
            healthInfo.put("cluster", getClusterMetrics());

            // 5. 缓存统计
            healthInfo.put("cache", getCacheStatistics());

            // 6. 慢查询统计
            healthInfo.put("slowlog", getSlowLogMetrics());

            healthInfo.put("status", "UP");
            healthInfo.put("timestamp", System.currentTimeMillis());

        } catch (Exception e) {
            log.error("Redis健康检查失败: {}", e.getMessage(), e);
            healthInfo.put("status", "DOWN");
            healthInfo.put("error", e.getMessage());
        }

        return healthInfo;
    }

    /**
     * 基础连接检查
     */
    private Map<String, Object> checkConnection() {
        Map<String, Object> connection = new HashMap<>();
        try {
            // 执行ping命令
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();
            connection.put("ping", pong);
            connection.put("status", "UP");
            
            // 获取连接信息
            connection.put("connectionType", redisTemplate.getConnectionFactory().getClass().getSimpleName());
        } catch (Exception e) {
            connection.put("status", "DOWN");
            connection.put("error", e.getMessage());
        }
        return connection;
    }

    /**
     * 获取性能指标
     */
    private Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        try {
            long startTime = System.currentTimeMillis();

            // 测试写入性能
            String testKey = "perf:test:" + startTime;
            redisTemplate.opsForValue().set(testKey, "test", 10);
            long writeLatency = System.currentTimeMillis() - startTime;

            // 测试读取性能
            startTime = System.currentTimeMillis();
            redisTemplate.opsForValue().get(testKey);
            long readLatency = System.currentTimeMillis() - startTime;

            // 测试删除性能
            startTime = System.currentTimeMillis();
            redisTemplate.delete(testKey);
            long deleteLatency = System.currentTimeMillis() - startTime;

            metrics.put("writeLatency", writeLatency + "ms");
            metrics.put("readLatency", readLatency + "ms");
            metrics.put("deleteLatency", deleteLatency + "ms");

            // 获取Redis info统计
            Properties info = redisTemplate.getConnectionFactory().getConnection().info("stats");
            if (info != null) {
                metrics.put("totalCommandsProcessed", info.getProperty("total_commands_processed"));
                metrics.put("instantaneousOpsPerSec", info.getProperty("instantaneous_ops_per_sec"));
                metrics.put("keyspaceHits", info.getProperty("keyspace_hits"));
                metrics.put("keyspaceMisses", info.getProperty("keyspace_misses"));

                // 计算命中率
                long hits = Long.parseLong(info.getProperty("keyspace_hits", "0"));
                long misses = Long.parseLong(info.getProperty("keyspace_misses", "0"));
                if (hits + misses > 0) {
                    double hitRate = (double) hits / (hits + misses) * 100;
                    metrics.put("hitRate", String.format("%.2f%%", hitRate));
                }
            }

        } catch (Exception e) {
            metrics.put("error", e.getMessage());
        }

        return metrics;
    }

    /**
     * 获取内存指标
     */
    private Map<String, Object> getMemoryMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        try {
            Properties info = redisTemplate.getConnectionFactory().getConnection().info("memory");
            if (info != null) {
                metrics.put("usedMemory", info.getProperty("used_memory_human"));
                metrics.put("usedMemoryPeak", info.getProperty("used_memory_peak_human"));
                metrics.put("memFragmentationRatio", info.getProperty("mem_fragmentation_ratio"));
                metrics.put("usedMemoryRss", info.getProperty("used_memory_rss_human"));
            }
        } catch (Exception e) {
            metrics.put("error", e.getMessage());
        }

        return metrics;
    }

    /**
     * 获取集群状态
     */
    private Map<String, Object> getClusterMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        try {
            Properties info = redisTemplate.getConnectionFactory().getConnection().info("cluster");
            if (info != null) {
                metrics.put("clusterEnabled", info.getProperty("cluster_enabled"));
                
                // 如果是集群模式，获取更多集群信息
                if ("1".equals(info.getProperty("cluster_enabled"))) {
                    metrics.put("clusterState", info.getProperty("cluster_state"));
                    metrics.put("clusterSlotsAssigned", info.getProperty("cluster_slots_assigned"));
                    metrics.put("clusterSlotsOk", info.getProperty("cluster_slots_ok"));
                    metrics.put("clusterSlotsPfail", info.getProperty("cluster_slots_pfail"));
                    metrics.put("clusterSlotsFail", info.getProperty("cluster_slots_fail"));
                }
            }
        } catch (Exception e) {
            metrics.put("error", e.getMessage());
        }

        return metrics;
    }
    
    /**
     * 获取缓存统计
     */
    private Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // 本地缓存统计
            if (multiLevelCacheService != null) {
                stats.put("localCache", multiLevelCacheService.getCacheStats());
            }

            // Redis键空间统计
            Properties info = redisTemplate.getConnectionFactory().getConnection().info("keyspace");
            if (info != null) {
                stats.put("keyspace", info);
            }

        } catch (Exception e) {
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    /**
     * 获取慢查询指标
     */
    private Map<String, Object> getSlowLogMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        try {
            // 获取慢查询数量
            Long slowLogLen = redisTemplate.getConnectionFactory().getConnection().slowLogLen();
            metrics.put("slowLogCount", slowLogLen);

            if (slowLogLen > 0) {
                // 获取最近的慢查询
                java.util.List<Object> slowLogs = redisTemplate.getConnectionFactory().getConnection().slowLogGet(5);
                metrics.put("recentSlowLogs", slowLogs);
            }

        } catch (Exception e) {
            metrics.put("error", e.getMessage());
        }

        return metrics;
    }
    
    /**
     * 兼容原有接口
     */
    public Map<String, Object> checkRedisHealth() {
        Map<String, Object> healthInfo = new HashMap<>();

        try {
            // 执行ping命令
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();
            healthInfo.put("status", "UP");
            healthInfo.put("ping", pong);

            // 获取基本信息
            Map<String, Object> connection = checkConnection();
            connection.remove("status"); // 避免重复
            healthInfo.put("info", connection);

        } catch (Exception e) {
            log.error("Redis健康检查失败: {}", e.getMessage(), e);
            healthInfo.put("status", "DOWN");
            healthInfo.put("error", e.getMessage());
        }

        return healthInfo;
    }
} 