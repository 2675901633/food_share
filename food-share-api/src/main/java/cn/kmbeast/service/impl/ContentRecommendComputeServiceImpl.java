package cn.kmbeast.service.impl;

import cn.kmbeast.mapper.ContentFeatureMapper;
import cn.kmbeast.mapper.ContentSimilarityMapper;
import cn.kmbeast.mapper.GourmetMapper;
import cn.kmbeast.mapper.UserBehaviorMapper;
import cn.kmbeast.pojo.entity.ContentFeature;
import cn.kmbeast.pojo.entity.ContentSimilarity;
import cn.kmbeast.pojo.vo.GourmetVO;
import cn.kmbeast.service.ContentRecommendComputeService;
import cn.kmbeast.utils.CacheConstants;
import cn.kmbeast.utils.RedisUtil;
import cn.kmbeast.utils.TextUtils;
import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 内容推荐计算服务实现类
 */
@Service
@Slf4j
public class ContentRecommendComputeServiceImpl implements ContentRecommendComputeService {
    @Autowired
    private GourmetMapper gourmetMapper;

    @Autowired
    private ContentFeatureMapper contentFeatureMapper;

    @Autowired
    private ContentSimilarityMapper contentSimilarityMapper;

    @Autowired
    private UserBehaviorMapper userBehaviorMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisUtil redisUtil;

    // 特征权重配置
    private static final double CATEGORY_WEIGHT = 2.0; // 分类特征权重
    private static final double TITLE_WEIGHT = 1.5; // 标题特征权重
    private static final double CONTENT_WEIGHT = 0.6; // 内容特征权重
    private static final double TAG_WEIGHT = 0.7; // 标签特征权重（如果有）

    // 特征数量配置
    private static final int TITLE_FEATURES_LIMIT = 10; // 标题特征数量
    private static final int CONTENT_FEATURES_LIMIT = 50; // 内容特征数量

    // 用户行为权重配置
    private static final double VIEW_WEIGHT = 0.3; // 浏览权重
    private static final double LIKE_WEIGHT = 0.6; // 点赞权重
    private static final double COMMENT_WEIGHT = 0.8; // 评论权重
    private static final double COLLECT_WEIGHT = 1.0; // 收藏权重

    // 缓存锁前缀
    private static final String COMPUTE_LOCK_PREFIX = "lock:compute:";

    /**
     * 压缩特征向量
     * 1. 移除接近0的特征
     * 2. 使用更紧凑的键名
     */
    private Map<String, Double> compressFeatures(Map<String, Double> features) {
        Map<String, Double> compressed = new HashMap<>();
        features.forEach((key, value) -> {
            // 移除接近0的特征
            if (Math.abs(value) > 0.01) {
                // 压缩键名
                String compressedKey = key
                        .replace("category_", "c_")
                        .replace("title_", "t_")
                        .replace("content_", "n_");
                compressed.put(compressedKey, value);
            }
        });
        return compressed;
    }

    @Override
    @Transactional
    public boolean computeAndSaveFeatures(Integer gourmetId) {
        try {
            // 检查缓存中是否已有特征数据
            String featureCacheKey = CacheConstants.CONTENT_FEATURES_KEY_PREFIX + gourmetId;
            if (redisUtil.hasKey(featureCacheKey)) {
                // 特征数据已存在于缓存中，直接返回成功
                log.info("Feature data for gourmet {} already exists in cache", gourmetId);
                return true;
            }

            // 使用分布式锁防止并发计算
            String lockKey = COMPUTE_LOCK_PREFIX + "feature:" + gourmetId;
            boolean lockAcquired = redisUtil.set(lockKey, "1", 30); // 锁30秒

            if (!lockAcquired) {
                log.info("Another process is computing features for gourmet {}, skipping", gourmetId);
                return true; // 另一个进程正在计算，返回成功
            }

            try {
                // 1. 获取美食信息
                GourmetVO gourmet = gourmetMapper.queryById(gourmetId);
                if (gourmet == null) {
                    log.warn("Gourmet not found with id: {}", gourmetId);
                    return false;
                }

                // 2. 提取特征
                Map<String, Double> features = new HashMap<>();

                // 2.1 分类特征
                if (StringUtils.hasText(gourmet.getCategoryName())) {
                    // 主分类特征
                    features.put("category_" + gourmet.getCategoryName(), CATEGORY_WEIGHT);
                    // 分类词特征（分词后的每个词也作为特征）
                    Map<String, Integer> categoryTerms = TextUtils.extractTerms(gourmet.getCategoryName());
                    categoryTerms.entrySet().stream()
                            .filter(e -> !TextUtils.isStopWord(e.getKey()))
                            .forEach(e -> features.put(
                                    "category_term_" + e.getKey(),
                                    e.getValue() * CATEGORY_WEIGHT * 0.5 // 分类词特征权重为主分类的一半
                            ));
                }

                // 2.2 标题特征
                if (StringUtils.hasText(gourmet.getTitle())) {
                    try {
                        Map<String, Integer> titleTerms = TextUtils.extractTerms(gourmet.getTitle());

                        // 单个词特征
                        titleTerms.entrySet().stream()
                                .filter(e -> !TextUtils.isStopWord(e.getKey()))
                                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                                .limit(TITLE_FEATURES_LIMIT)
                                .forEach(e -> features.put(
                                        "title_" + e.getKey(),
                                        e.getValue() * TITLE_WEIGHT));

                        // 词组合特征（相邻词的组合）
                        List<String> titleWords = new ArrayList<>(titleTerms.keySet());
                        for (int i = 0; i < titleWords.size() - 1; i++) {
                            String word1 = titleWords.get(i);
                            String word2 = titleWords.get(i + 1);
                            if (!TextUtils.isStopWord(word1) && !TextUtils.isStopWord(word2)) {
                                features.put(
                                        "title_phrase_" + word1 + "_" + word2,
                                        TITLE_WEIGHT * 0.8 // 词组特征权重稍低
                                );
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Failed to extract title features for gourmet: {}", gourmetId, e);
                    }
                }

                // 2.3 内容特征
                if (StringUtils.hasText(gourmet.getContent())) {
                    try {
                        Map<String, Integer> contentTerms = TextUtils.extractTerms(gourmet.getContent());
                        contentTerms.entrySet().stream()
                                .filter(e -> !TextUtils.isStopWord(e.getKey()))
                                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                                .limit(CONTENT_FEATURES_LIMIT)
                                .forEach(e -> features.put(
                                        "content_" + e.getKey(),
                                        e.getValue() * CONTENT_WEIGHT));
                    } catch (Exception e) {
                        log.warn("Failed to extract content features for gourmet: {}", gourmetId, e);
                    }
                }

                // 3. 确保特征不为空
                if (features.isEmpty()) {
                    log.warn("No features extracted for gourmet: {}, using default feature", gourmetId);
                    features.put("default", 1.0);
                }

                // 4. L2归一化
                double l2Norm = Math.sqrt(features.values().stream()
                        .mapToDouble(v -> v * v)
                        .sum());
                if (l2Norm > 0) {
                    features.replaceAll((k, v) -> v / l2Norm);
                }

                // 5. 保存特征
                LocalDateTime now = LocalDateTime.now();
                ContentFeature feature = new ContentFeature();
                feature.setGourmetId(gourmetId);
                feature.setFeatureVector(objectMapper.writeValueAsString(features));
                feature.setCreateTime(now);
                feature.setUpdateTime(now);

                contentFeatureMapper.insertOrUpdate(feature);

                // 6. 缓存特征数据
                redisUtil.set(featureCacheKey, JSON.toJSONString(features), CacheConstants.CONTENT_FEATURES_EXPIRE);

                log.info("Successfully saved features for gourmet: {}", gourmetId);
                return true;
            } finally {
                // 释放锁
                redisUtil.del(lockKey);
            }
        } catch (Exception e) {
            log.error("Failed to compute and save features for gourmet: {}", gourmetId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public void updateRecommendations(Integer gourmetId) {
        log.info("Updating recommendations for gourmet: {}", gourmetId);

        try {
            // 使用分布式锁防止并发计算
            String lockKey = COMPUTE_LOCK_PREFIX + "recommend:" + gourmetId;
            boolean lockAcquired = redisUtil.set(lockKey, "1", 300); // 锁5分钟

            if (!lockAcquired) {
                log.info("Another process is updating recommendations for gourmet {}, skipping", gourmetId);
                return; // 另一个进程正在更新，直接返回
            }

            try {
                // 1. 验证源美食是否存在
                GourmetVO sourceGourmet = gourmetMapper.queryById(gourmetId);
                if (sourceGourmet == null) {
                    log.error("Source gourmet not found: {}", gourmetId);
                    return;
                }

                // 2. 计算源美食特征
                boolean success = computeAndSaveFeatures(gourmetId);
                if (!success) {
                    log.error("Failed to compute features for source gourmet: {}", gourmetId);
                    return;
                }

                // 3. 获取所有其他美食ID
                List<Integer> allIds = gourmetMapper.getAllIdsExcept(gourmetId);
                if (allIds.isEmpty()) {
                    log.info("No other gourmet items found");
                    return;
                }

                // 4. 为每个目标美食计算特征和相似度
                for (Integer targetId : allIds) {
                    try {
                        // 4.1 验证目标美食
                        GourmetVO targetGourmet = gourmetMapper.queryById(targetId);
                        if (targetGourmet == null) {
                            continue;
                        }

                        // 4.2 计算目标美食特征
                        if (computeAndSaveFeatures(targetId)) {
                            // 4.3 计算相似度
                            computeAndSaveSimilarity(gourmetId, targetId);
                        }
                    } catch (Exception e) {
                        log.error("Failed to process similarity for {} -> {}", gourmetId, targetId, e);
                    }
                }

                // 5. 清除相关缓存，以便重新生成
                String similarCacheKeyPattern = CacheConstants.SIMILAR_CONTENT_KEY_PREFIX + gourmetId + "*";
                Set<String> keys = redisUtil.keys(similarCacheKeyPattern);
                if (keys != null && !keys.isEmpty()) {
                    for (String key : keys) {
                        redisUtil.del(key);
                    }
                }

                log.info("Successfully updated recommendations for gourmet: {}", gourmetId);
            } finally {
                // 释放锁
                redisUtil.del(lockKey);
            }
        } catch (Exception e) {
            log.error("Failed to update recommendations for gourmet: {}", gourmetId, e);
        }
    }

    @Override
    @Transactional
    public BigDecimal computeAndSaveSimilarity(Integer sourceId, Integer targetId) {
        try {
            // 1. 获取内容特征相似度
            double contentSimilarity = computeContentSimilarity(sourceId, targetId);

            // 2. 获取用户行为相似度
            double behaviorSimilarity = computeUserBehaviorSimilarity(sourceId, targetId);

            // 3. 综合两种相似度（可以调整权重）
            double finalSimilarity = 0.7 * contentSimilarity + 0.3 * behaviorSimilarity;

            // 4. 保存相似度
            ContentSimilarity similarityEntity = new ContentSimilarity();
            similarityEntity.setSourceId(sourceId);
            similarityEntity.setTargetId(targetId);
            similarityEntity.setSimilarity(BigDecimal.valueOf(finalSimilarity));
            similarityEntity.setCreateTime(LocalDateTime.now());

            contentSimilarityMapper.insertOrUpdate(similarityEntity);

            return BigDecimal.valueOf(finalSimilarity);
        } catch (Exception e) {
            log.error("Failed to compute similarity between {} and {}", sourceId, targetId, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 计算基于用户行为的相似度
     */
    private double computeUserBehaviorSimilarity(Integer sourceId, Integer targetId) {
        try {
            // 尝试从缓存获取用户行为相似度
            String cacheKey = "behavior:similarity:" + sourceId + ":" + targetId;
            Object cachedValue = redisUtil.get(cacheKey);
            if (cachedValue != null) {
                try {
                    return Double.parseDouble(cachedValue.toString());
                } catch (NumberFormatException e) {
                    log.warn("Invalid cached behavior similarity: {}", cachedValue);
                }
            }

            // 1. 获取与这两个美食交互过的用户列表
            List<Integer> sourceUserIds = userBehaviorMapper.getUsersByGourmet(sourceId);
            List<Integer> targetUserIds = userBehaviorMapper.getUsersByGourmet(targetId);

            // 如果没有用户交互，返回0
            if (sourceUserIds.isEmpty() || targetUserIds.isEmpty()) {
                return 0.0;
            }

            // 2. 计算用户交集
            Set<Integer> commonUsers = new HashSet<>(sourceUserIds);
            commonUsers.retainAll(targetUserIds);

            if (commonUsers.isEmpty()) {
                return 0.0;
            }

            // 3. 计算用户行为相似度
            double similarity = 0.0;
            for (Integer userId : commonUsers) {
                // 获取用户对两个美食的行为分数
                double sourceScore = calculateUserBehaviorScore(userId, sourceId);
                double targetScore = calculateUserBehaviorScore(userId, targetId);
                similarity += sourceScore * targetScore;
            }

            // 4. 归一化相似度
            double norm1 = Math.sqrt(sourceUserIds.size());
            double norm2 = Math.sqrt(targetUserIds.size());
            double result = similarity / (norm1 * norm2);

            // 5. 缓存计算结果
            redisUtil.set(cacheKey, String.valueOf(result), 3600); // 缓存1小时

            return result;
        } catch (Exception e) {
            log.error("Failed to compute user behavior similarity between {} and {}", sourceId, targetId, e);
            return 0.0;
        }
    }

    /**
     * 计算用户对某个美食的行为分数
     */
    private double calculateUserBehaviorScore(Integer userId, Integer gourmetId) {
        // 获取用户行为统计
        int viewCount = userBehaviorMapper.getViewCount(userId, gourmetId);
        int likeCount = userBehaviorMapper.getLikeCount(userId, gourmetId);
        int commentCount = userBehaviorMapper.getCommentCount(userId, gourmetId);
        int collectCount = userBehaviorMapper.getCollectCount(userId, gourmetId);

        // 计算加权分数
        return viewCount * VIEW_WEIGHT +
                likeCount * LIKE_WEIGHT +
                commentCount * COMMENT_WEIGHT +
                collectCount * COLLECT_WEIGHT;
    }

    /**
     * 计算内容特征相似度
     */
    private double computeContentSimilarity(Integer sourceId, Integer targetId) {
        try {
            // 尝试从缓存获取内容相似度
            String cacheKey = "content:similarity:" + sourceId + ":" + targetId;
            Object cachedValue = redisUtil.get(cacheKey);
            if (cachedValue != null) {
                try {
                    return Double.parseDouble(cachedValue.toString());
                } catch (NumberFormatException e) {
                    log.warn("Invalid cached content similarity: {}", cachedValue);
                }
            }

            // 从缓存或数据库获取特征
            Map<String, Double> sourceFeatures = getFeatures(sourceId);
            Map<String, Double> targetFeatures = getFeatures(targetId);

            if (sourceFeatures.isEmpty() || targetFeatures.isEmpty()) {
                return 0.0;
            }

            double result = calculateCosineSimilarity(sourceFeatures, targetFeatures);

            // 缓存计算结果
            redisUtil.set(cacheKey, String.valueOf(result), 3600); // 缓存1小时

            return result;
        } catch (Exception e) {
            log.error("Failed to compute content similarity between {} and {}", sourceId, targetId, e);
            return 0.0;
        }
    }

    /**
     * 获取美食的特征向量，优先从缓存获取
     */
    private Map<String, Double> getFeatures(Integer gourmetId) {
        try {
            // 1. 尝试从缓存获取
            String cacheKey = CacheConstants.CONTENT_FEATURES_KEY_PREFIX + gourmetId;
            Object cachedData = redisUtil.get(cacheKey);

            if (cachedData != null) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> rawCachedFeatures = JSON.parseObject(cachedData.toString(), Map.class);
                    // 转换为Double类型
                    Map<String, Double> cachedFeatures = new HashMap<>();
                    for (Map.Entry<String, Object> entry : rawCachedFeatures.entrySet()) {
                        Object value = entry.getValue();
                        if (value instanceof Number) {
                            cachedFeatures.put(entry.getKey(), ((Number) value).doubleValue());
                        } else {
                            cachedFeatures.put(entry.getKey(), 0.0);
                        }
                    }
                    return cachedFeatures;
                } catch (Exception e) {
                    log.warn("Failed to parse cached features for gourmet {}: {}", gourmetId, e.getMessage());
                }
            }

            // 2. 从数据库获取
            ContentFeature feature = contentFeatureMapper.selectByGourmetId(gourmetId);
            if (feature == null || !StringUtils.hasText(feature.getFeatureVector())) {
                return Collections.emptyMap();
            }

            // 3. 解析特征
            Map<String, Object> rawFeatures = objectMapper.readValue(
                    feature.getFeatureVector(),
                    new TypeReference<Map<String, Object>>() {
                    });

            // 4. 转换为Double类型
            Map<String, Double> features = new HashMap<>();
            for (Map.Entry<String, Object> entry : rawFeatures.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Number) {
                    features.put(entry.getKey(), ((Number) value).doubleValue());
                } else {
                    features.put(entry.getKey(), 0.0);
                }
            }

            // 5. 缓存特征
            redisUtil.set(cacheKey, JSON.toJSONString(features), CacheConstants.CONTENT_FEATURES_EXPIRE);

            return features;
        } catch (Exception e) {
            log.error("Failed to get features for gourmet {}: {}", gourmetId, e.getMessage());
            return Collections.emptyMap();
        }
    }

    @Override
    @Transactional
    public void batchUpdateRecommendations(List<Integer> gourmetIds) {
        log.info("Batch updating recommendations for {} gourmets", gourmetIds.size());

        for (Integer gourmetId : gourmetIds) {
            try {
                updateRecommendations(gourmetId);
            } catch (Exception e) {
                log.error("Failed to update recommendations for gourmet: {}", gourmetId, e);
            }
        }
    }

    /**
     * 检查特征是否过期（超过7天）
     */
    private boolean isFeatureExpired(ContentFeature feature) {
        return feature.getUpdateTime()
                .plusDays(7)
                .isBefore(LocalDateTime.now());
    }

    /**
     * 计算余弦相似度
     */
    private double calculateCosineSimilarity(Map<String, Double> features1, Map<String, Double> features2) {
        Set<String> allTerms = new HashSet<>();
        allTerms.addAll(features1.keySet());
        allTerms.addAll(features2.keySet());

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (String term : allTerms) {
            double value1 = features1.getOrDefault(term, 0.0);
            double value2 = features2.getOrDefault(term, 0.0);

            dotProduct += value1 * value2;
            norm1 += value1 * value1;
            norm2 += value2 * value2;
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}
