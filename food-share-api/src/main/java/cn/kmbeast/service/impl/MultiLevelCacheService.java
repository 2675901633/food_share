package cn.kmbeast.service.impl;

import cn.kmbeast.utils.RedisUtil;
import com.alibaba.fastjson.JSON;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * 多级缓存服务
 * L1: 本地Caffeine缓存
 * L2: Redis分布式缓存
 */
@Slf4j
@Service
public class MultiLevelCacheService {

    @Autowired
    private RedisUtil redisUtil;

    private Cache<String, Object> localCache;

    @PostConstruct
    public void initLocalCache() {
        localCache = Caffeine.newBuilder()
                .maximumSize(10000) // 最大缓存10000个对象
                .expireAfterWrite(5, TimeUnit.MINUTES) // 写入后5分钟过期
                .expireAfterAccess(2, TimeUnit.MINUTES) // 访问后2分钟过期
                .recordStats() // 启用统计
                .build();

        log.info("本地缓存初始化完成");
    }

    /**
     * 获取缓存数据
     * 先查本地缓存，再查Redis缓存
     */
    public <T> T get(String key, Class<T> clazz) {
        // L1: 本地缓存
        Object localValue = localCache.getIfPresent(key);
        if (localValue != null) {
            log.debug("本地缓存命中: {}", key);
            return (T) localValue;
        }

        // L2: Redis缓存
        Object redisValue = redisUtil.get(key);
        if (redisValue != null) {
            log.debug("Redis缓存命中: {}", key);
            T result = JSON.parseObject(redisValue.toString(), clazz);
            // 回写到本地缓存
            localCache.put(key, result);
            return result;
        }

        log.debug("多级缓存未命中: {}", key);
        return null;
    }

    /**
     * 设置缓存数据
     * 同时写入本地缓存和Redis缓存
     */
    public void set(String key, Object value, long redisExpire) {
        // 写入Redis
        redisUtil.set(key, JSON.toJSONString(value), redisExpire);

        // 写入本地缓存
        localCache.put(key, value);

        log.debug("多级缓存写入: {}", key);
    }

    /**
     * 删除缓存
     */
    public void delete(String key) {
        localCache.invalidate(key);
        redisUtil.del(key);
        log.debug("多级缓存删除: {}", key);
    }

    /**
     * 获取缓存统计信息
     */
    public String getCacheStats() {
        return localCache.stats().toString();
    }
} 