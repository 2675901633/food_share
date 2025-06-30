package cn.kmbeast.service.impl;

import cn.kmbeast.context.LocalThreadHolder;
import cn.kmbeast.mapper.CategoryMapper;
import cn.kmbeast.mapper.GourmetMapper;
import cn.kmbeast.mapper.InteractionMapper;
import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.extend.GourmetQueryDto;
import cn.kmbeast.pojo.dto.query.extend.CategoryQueryDto;
import cn.kmbeast.pojo.dto.query.extend.InteractionQueryDto;
import cn.kmbeast.pojo.em.InteractionTypeEnum;
import cn.kmbeast.pojo.entity.Category;
import cn.kmbeast.pojo.entity.Interaction;
import cn.kmbeast.pojo.vo.GourmetVO;
import cn.kmbeast.pojo.vo.UserPreferenceVO;
import cn.kmbeast.service.UserBehaviorService;
import cn.kmbeast.utils.CacheConstants;
import cn.kmbeast.utils.RedisUtil;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户行为服务实现类
 */
@Slf4j
@Service
public class UserBehaviorServiceImpl implements UserBehaviorService {

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private GourmetMapper gourmetMapper;

    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private InteractionMapper interactionMapper;

    /**
     * 记录用户浏览美食的行为
     *
     * @param userId     用户ID
     * @param gourmetId  美食ID
     * @param categoryId 分类ID
     * @return 操作结果
     */
    @Override
    public Result<Void> recordUserView(Integer userId, Integer gourmetId, Integer categoryId) {
        try {
            if (userId == null || gourmetId == null) {
                return ApiResult.error("参数错误");
            }

            long timestamp = System.currentTimeMillis();

            // 1. 记录用户浏览历史 - 使用ZSet按时间戳排序
            String historyKey = CacheConstants.USER_HISTORY_KEY_PREFIX + userId;
            redisUtil.zAdd(historyKey, gourmetId.toString(), timestamp);
            redisUtil.expire(historyKey, CacheConstants.USER_HISTORY_EXPIRE);

            // 2. 更新用户最近浏览 - 使用ZSet，保持最新的记录
            String recentKey = CacheConstants.USER_RECENT_VIEW_KEY_PREFIX + userId;
            redisUtil.zAdd(recentKey, gourmetId.toString(), timestamp);
            redisUtil.expire(recentKey, CacheConstants.USER_RECENT_VIEW_EXPIRE);

            // 3. 限制浏览历史和最近浏览的数量
            long historySize = redisUtil.zSize(historyKey);
            if (historySize > CacheConstants.USER_HISTORY_MAX_SIZE) {
                redisUtil.zRemoveRange(historyKey, 0, historySize - CacheConstants.USER_HISTORY_MAX_SIZE - 1);
            }

            long recentSize = redisUtil.zSize(recentKey);
            if (recentSize > CacheConstants.USER_RECENT_VIEW_MAX_SIZE) {
                redisUtil.zRemoveRange(recentKey, 0, recentSize - CacheConstants.USER_RECENT_VIEW_MAX_SIZE - 1);
            }

            // 4. 更新用户分类偏好
            if (categoryId != null) {
                updateUserCategoryPreference(userId, categoryId);
            }

            return ApiResult.success();
        } catch (Exception e) {
            log.error("记录用户浏览行为失败: {}", e.getMessage(), e);
            return ApiResult.error("记录用户浏览行为失败");
        }
    }

    /**
     * 获取用户浏览历史
     *
     * @param userId 用户ID
     * @param limit  返回记录数量限制
     * @return 用户浏览历史列表
     */
    @Override
    public Result<List<GourmetVO>> getUserViewHistory(Integer userId, Integer limit) {
        try {
            if (userId == null) {
                userId = LocalThreadHolder.getUserId();
            }
            if (limit == null || limit <= 0) {
                limit = CacheConstants.USER_HISTORY_MAX_SIZE;
            }

            String historyKey = CacheConstants.USER_HISTORY_KEY_PREFIX + userId;
            Set<String> gourmetIds = redisUtil.zReverseRange(historyKey, 0, limit - 1);

            if (CollectionUtils.isEmpty(gourmetIds)) {
                return ApiResult.success(new ArrayList<>());
            }

            List<Integer> ids = gourmetIds.stream()
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());

            List<GourmetVO> gourmetVOList = gourmetMapper.queryByIds(ids);

            // 按照Redis中的顺序重新排序结果
            Map<Integer, GourmetVO> gourmetMap = gourmetVOList.stream()
                    .collect(Collectors.toMap(GourmetVO::getId, gourmet -> gourmet));

            List<GourmetVO> sortedResult = ids.stream()
                    .map(gourmetMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return ApiResult.success(sortedResult);
        } catch (Exception e) {
            log.error("获取用户浏览历史失败: {}", e.getMessage(), e);
            return ApiResult.error("获取用户浏览历史失败");
        }
    }

    /**
     * 获取用户最近浏览的美食
     *
     * @param userId 用户ID
     * @param limit  返回记录数量限制
     * @return 最近浏览的美食列表
     */
    @Override
    public Result<List<GourmetVO>> getUserRecentViews(Integer userId, Integer limit) {
        try {
            if (userId == null) {
                userId = LocalThreadHolder.getUserId();
            }
            if (limit == null || limit <= 0) {
                limit = CacheConstants.USER_RECENT_VIEW_MAX_SIZE;
            }

            String recentKey = CacheConstants.USER_RECENT_VIEW_KEY_PREFIX + userId;
            Set<String> gourmetIds = redisUtil.zReverseRange(recentKey, 0, limit - 1);

            if (CollectionUtils.isEmpty(gourmetIds)) {
                return ApiResult.success(new ArrayList<>());
            }

            List<Integer> ids = gourmetIds.stream()
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());

            List<GourmetVO> gourmetVOList = gourmetMapper.queryByIds(ids);

            // 按照Redis中的顺序重新排序结果
            Map<Integer, GourmetVO> gourmetMap = gourmetVOList.stream()
                    .collect(Collectors.toMap(GourmetVO::getId, gourmet -> gourmet));

            List<GourmetVO> sortedResult = ids.stream()
                    .map(gourmetMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return ApiResult.success(sortedResult);
        } catch (Exception e) {
            log.error("获取用户最近浏览失败: {}", e.getMessage(), e);
            return ApiResult.error("获取用户最近浏览失败");
        }
    }

    /**
     * 获取用户偏好数据
     *
     * @param userId 用户ID
     * @return 用户偏好数据
     */
    @Override
    public Result<UserPreferenceVO> getUserPreferences(Integer userId) {
        try {
            if (userId == null) {
                userId = LocalThreadHolder.getUserId();
            }

            String preferencesKey = CacheConstants.USER_PREFERENCES_KEY_PREFIX + userId;
            Map<Object, Object> rawPreferences = redisUtil.hGetAll(preferencesKey);

            if (CollectionUtils.isEmpty(rawPreferences)) {
                return ApiResult.success(new UserPreferenceVO(userId, new HashMap<>(), 0, null, null));
            }

            // 转换偏好数据类型
            Map<Integer, Double> categoryPreferences = new HashMap<>();
            for (Map.Entry<Object, Object> entry : rawPreferences.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    try {
                        Integer categoryId = Integer.parseInt(entry.getKey().toString());
                        Double score = Double.parseDouble(entry.getValue().toString());
                        categoryPreferences.put(categoryId, score);
                    } catch (NumberFormatException e) {
                        log.warn("用户偏好数据格式错误: key={}, value={}", entry.getKey(), entry.getValue());
                    }
                }
            }

            // 找出最喜欢的分类
            Integer favoriteCategory = null;
            String favoriteCategoryName = null;
            Double maxScore = 0.0;

            for (Map.Entry<Integer, Double> entry : categoryPreferences.entrySet()) {
                if (entry.getValue() > maxScore) {
                    maxScore = entry.getValue();
                    favoriteCategory = entry.getKey();
                }
            }

            // 获取分类名称
            if (favoriteCategory != null) {
                // 直接使用查询方法获取分类
                CategoryQueryDto categoryQueryDto = new CategoryQueryDto();
                List<Category> categories = categoryMapper.query(categoryQueryDto);
                for (Category category : categories) {
                    if (category.getId().equals(favoriteCategory)) {
                        favoriteCategoryName = category.getName();
                        break;
                    }
                }
            }

            // 获取总浏览数量
            String historyKey = CacheConstants.USER_HISTORY_KEY_PREFIX + userId;
            long totalViewCount = redisUtil.zSize(historyKey);

            UserPreferenceVO preferenceVO = new UserPreferenceVO(
                    userId,
                    categoryPreferences,
                    (int) totalViewCount,
                    favoriteCategory,
                    favoriteCategoryName);

            return ApiResult.success(preferenceVO);
        } catch (Exception e) {
            log.error("获取用户偏好数据失败: {}", e.getMessage(), e);
            return ApiResult.error("获取用户偏好数据失败");
        }
    }

    /**
     * 更新用户分类偏好
     *
     * @param userId     用户ID
     * @param categoryId 分类ID
     * @return 操作结果
     */
    @Override
    public Result<Void> updateUserCategoryPreference(Integer userId, Integer categoryId) {
        try {
            if (userId == null || categoryId == null) {
                return ApiResult.error("参数错误");
            }

            String preferencesKey = CacheConstants.USER_PREFERENCES_KEY_PREFIX + userId;

            // 获取当前分类的偏好分数
            Double currentScore = 0.0;
            Object scoreObj = redisUtil.hGet(preferencesKey, categoryId.toString());
            if (scoreObj != null) {
                try {
                    currentScore = Double.parseDouble(scoreObj.toString());
                } catch (NumberFormatException e) {
                    log.warn("用户偏好分数格式错误: {}", scoreObj);
                }
            }

            // 增加偏好分数（每次浏览增加0.1）
            double newScore = currentScore + 0.1;
            redisUtil.hSet(preferencesKey, categoryId.toString(), String.valueOf(newScore));
            redisUtil.expire(preferencesKey, CacheConstants.USER_PREFERENCES_EXPIRE);

            return ApiResult.success();
        } catch (Exception e) {
            log.error("更新用户分类偏好失败: {}", e.getMessage(), e);
            return ApiResult.error("更新用户分类偏好失败");
        }
    }

    /**
     * 清除用户浏览历史
     *
     * @param userId 用户ID
     * @return 操作结果
     */
    @Override
    public Result<Void> clearUserViewHistory(Integer userId) {
        try {
            if (userId == null) {
                userId = LocalThreadHolder.getUserId();
            }

            String historyKey = CacheConstants.USER_HISTORY_KEY_PREFIX + userId;
            String recentKey = CacheConstants.USER_RECENT_VIEW_KEY_PREFIX + userId;

            redisUtil.del(historyKey);
            redisUtil.del(recentKey);

            return ApiResult.success();
        } catch (Exception e) {
            log.error("清除用户浏览历史失败: {}", e.getMessage(), e);
            return ApiResult.error("清除用户浏览历史失败");
        }
    }

    /**
     * 同步用户行为数据到数据库
     * 定期将Redis中的用户行为数据持久化到MySQL
     *
     * @return 操作结果
     */
    @Override
    public Result<Void> syncUserBehaviorToDatabase() {
        try {
            log.info("开始同步用户行为数据到数据库...");

            // 获取所有用户历史记录的键
            Set<String> historyKeys = redisUtil.keys(CacheConstants.USER_HISTORY_KEY_PREFIX + "*");
            if (CollectionUtils.isEmpty(historyKeys)) {
                log.info("没有用户行为数据需要同步");
                return ApiResult.success();
            }

            int totalSynced = 0;

            // 遍历每个用户的历史记录
            for (String historyKey : historyKeys) {
                try {
                    // 从键中提取用户ID
                    String userIdStr = historyKey.substring(CacheConstants.USER_HISTORY_KEY_PREFIX.length());
                    Integer userId = Integer.parseInt(userIdStr);

                    // 获取用户浏览历史
                    Set<String> gourmetIdsWithScores = redisUtil.zRangeWithScores(historyKey);

                    for (String entry : gourmetIdsWithScores) {
                        String[] parts = entry.split(":");
                        if (parts.length == 2) {
                            String gourmetIdStr = parts[0];
                            String scoreStr = parts[1];

                            try {
                                Integer gourmetId = Integer.parseInt(gourmetIdStr);
                                Long timestamp = Long.parseLong(scoreStr);

                                // 检查数据库中是否已存在该记录
                                boolean exists = checkInteractionExists(userId, gourmetId);

                                if (!exists) {
                                    // 创建新的交互记录
                                    Interaction interaction = new Interaction();
                                    interaction.setUserId(userId);
                                    interaction.setContentId(gourmetId);
                                    interaction.setType(InteractionTypeEnum.VIEW.getType());
                                    interaction.setContentType("VIEW");
                                    interaction.setCreateTime(LocalDateTime.now());

                                    // 保存到数据库
                                    interactionMapper.save(interaction);
                                    totalSynced++;
                                }
                            } catch (NumberFormatException e) {
                                log.warn("解析浏览记录失败: gourmetId={}, score={}", gourmetIdStr, scoreStr);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("同步用户{}的行为数据失败: {}", historyKey, e.getMessage());
                }
            }

            log.info("用户行为数据同步完成，共同步{}条记录", totalSynced);
            return ApiResult.success();
        } catch (Exception e) {
            log.error("同步用户行为数据到数据库失败: {}", e.getMessage(), e);
            return ApiResult.error("同步用户行为数据到数据库失败");
        }
    }

    /**
     * 检查交互记录是否存在
     *
     * @param userId    用户ID
     * @param gourmetId 美食ID
     * @return 是否存在
     */
    private boolean checkInteractionExists(Integer userId, Integer gourmetId) {
        InteractionQueryDto queryDto = new InteractionQueryDto();
        queryDto.setUserId(userId);
        queryDto.setContentId(gourmetId);
        queryDto.setType(InteractionTypeEnum.VIEW.getType());

        Integer count = interactionMapper.queryCount(queryDto);
        return count != null && count > 0;
    }
}