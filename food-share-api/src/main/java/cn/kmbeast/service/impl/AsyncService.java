package cn.kmbeast.service.impl;

import cn.kmbeast.service.ContentRecommendComputeService;
import cn.kmbeast.service.GourmetService;
import cn.kmbeast.utils.CacheConstants;
import cn.kmbeast.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 异步处理服务
 */
@Slf4j
@Service
public class AsyncService {

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private GourmetService gourmetService;

    @Autowired
    private ContentRecommendComputeService contentRecommendComputeService;

    /**
     * 异步更新浏览量
     */
    @Async("taskExecutor")
    public void updateViewCountAsync(Integer gourmetId) {
        try {
            String viewKey = CacheConstants.GOURMET_VIEW_COUNT_PREFIX + gourmetId;
            redisUtil.incr(viewKey, 1);

            // 设置过期时间，定期同步到数据库
            redisUtil.expire(viewKey, CacheConstants.VIEW_COUNT_EXPIRE);

            log.debug("异步更新浏览量: gourmetId={}", gourmetId);
        } catch (Exception e) {
            log.error("异步更新浏览量失败: gourmetId={}, error={}", gourmetId, e.getMessage());
        }
    }

    /**
     * 异步计算推荐特征
     */
    @Async("taskExecutor")
    public void computeFeaturesAsync(Integer gourmetId) {
        try {
            contentRecommendComputeService.computeAndSaveFeatures(gourmetId);
            log.debug("异步计算特征完成: gourmetId={}", gourmetId);
        } catch (Exception e) {
            log.error("异步计算特征失败: gourmetId={}, error={}", gourmetId, e.getMessage());
        }
    }

    /**
     * 异步预热缓存
     */
    @Async("cacheWarmupExecutor")
    public void warmupCacheAsync(Integer gourmetId) {
        try {
            // 预热美食详情
            gourmetService.queryById(gourmetId);

            log.debug("异步预热缓存完成: gourmetId={}", gourmetId);
        } catch (Exception e) {
            log.error("异步预热缓存失败: gourmetId={}, error={}", gourmetId, e.getMessage());
        }
    }
} 