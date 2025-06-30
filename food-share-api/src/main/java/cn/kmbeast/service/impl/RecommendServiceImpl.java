package cn.kmbeast.service.impl;

import cn.kmbeast.context.LocalThreadHolder;
import cn.kmbeast.mapper.GourmetMapper;
import cn.kmbeast.mapper.InteractionMapper;
import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.extend.GourmetQueryDto;
import cn.kmbeast.pojo.dto.query.extend.InteractionQueryDto;
import cn.kmbeast.pojo.dto.query.extend.RatingDto;
import cn.kmbeast.pojo.em.InteractionTypeEnum;
import cn.kmbeast.pojo.vo.GourmetVO;
import cn.kmbeast.pojo.vo.InteractionVO;
import cn.kmbeast.service.RecommendService;
import cn.kmbeast.utils.CacheConstants;
import cn.kmbeast.utils.MahoutUtils;
import cn.kmbeast.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 推荐业务逻辑实现
 */
@Service
@Slf4j
public class RecommendServiceImpl implements RecommendService {

    @Resource
    private InteractionMapper interactionMapper;

    @Resource
    private GourmetMapper gourmetMapper;

    @Resource
    private RedisUtil redisUtil;

    /**
     * 查询需要推荐给用户的美食做法帖子数据
     *
     * @param item 需要的条数
     * @return Result<List<GourmetVO>>
     */
    @Override
    public Result<List<GourmetVO>> recommendGourmet(Integer item) {
        Integer userId = LocalThreadHolder.getUserId();
        if (userId == null) {
            return ApiResult.error("用户未登录");
        }

        // 1. 尝试从缓存获取用户推荐结果
        String cacheKey = CacheConstants.USER_RECOMMENDATIONS_KEY_PREFIX + userId;
        Object cachedData = redisUtil.get(cacheKey);

        if (cachedData != null) {
            try {
                @SuppressWarnings("unchecked")
                List<GourmetVO> cachedResult = (List<GourmetVO>) cachedData;
                // 如果缓存结果足够，直接返回
                if (cachedResult.size() >= item) {
                    return ApiResult.success(cachedResult.subList(0, item));
                }
            } catch (Exception e) {
                log.warn("Failed to parse cached recommendations: {}", e.getMessage());
            }
        }

        // 2. 缓存不存在或不足，计算推荐结果
        // 获取用户关于物品的评分数据
        InteractionQueryDto queryDto = new InteractionQueryDto();
        queryDto.setType(InteractionTypeEnum.RATING.getType());
        List<InteractionVO> interactionVOS = interactionMapper.query(queryDto);
        List<RatingDto> ratingDtoList = interactionVOS.stream().map(interactionVO -> new RatingDto(
                interactionVO.getUserId(),
                interactionVO.getContentId(),
                interactionVO.getScore())).collect(Collectors.toList());

        List<RecommendedItem> recommenderList = MahoutUtils.recommender(
                ratingDtoList,
                (long) userId,
                item);

        List<Long> gourmetIds = recommenderList.stream()
                .map(RecommendedItem::getItemID)
                .collect(Collectors.toList());

        List<GourmetVO> result;

        // 3. 如果没有计算出推荐结果，返回热门美食
        if (CollectionUtils.isEmpty(gourmetIds)) {
            // 尝试从热门排行榜获取
            Result<List<GourmetVO>> trendingResult = getTrendingFoods(item);
            if (trendingResult.isSuccess() && !CollectionUtils.isEmpty(trendingResult.getData())) {
                return trendingResult;
            }

            // 热门排行榜也没有，直接查询数据库
            GourmetQueryDto gourmetQueryDto = new GourmetQueryDto();
            gourmetQueryDto.setCurrent(0);
            gourmetQueryDto.setSize(item);
            result = gourmetMapper.queryByView(gourmetQueryDto);
        } else {
            // 将Long类型的ID转换为Integer
            List<Integer> ids = new ArrayList<>();
            for (Long gourmetId : gourmetIds) {
                ids.add(Integer.parseInt(String.valueOf(gourmetId)));
            }
            // 获取推荐的美食信息
            result = gourmetMapper.queryByIds(ids);
        }

        // 4. 缓存推荐结果
        if (!CollectionUtils.isEmpty(result)) {
            redisUtil.set(cacheKey, result, CacheConstants.USER_RECOMMENDATIONS_EXPIRE);
        }

        return ApiResult.success(result);
    }

    /**
     * 获取热门美食排行榜
     * 
     * @param limit 返回数量
     * @return 热门美食列表
     */
    @Override
    public Result<List<GourmetVO>> getTrendingFoods(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10; // 默认返回10条
        }

        try {
            // 1. 从Redis获取热门排行榜
            Map<Object, Double> trendingItems = redisUtil.zReverseRangeWithScores(
                    CacheConstants.TRENDING_FOODS_KEY, 0, limit - 1);

            if (trendingItems.isEmpty()) {
                // 排行榜为空，更新排行榜
                updateTrendingFoods();
                // 再次获取
                trendingItems = redisUtil.zReverseRangeWithScores(
                        CacheConstants.TRENDING_FOODS_KEY, 0, limit - 1);

                if (trendingItems.isEmpty()) {
                    // 仍然为空，直接查询数据库
                    GourmetQueryDto queryDto = new GourmetQueryDto();
                    queryDto.setCurrent(0);
                    queryDto.setSize(limit);
                    List<GourmetVO> result = gourmetMapper.queryByView(queryDto);
                    return ApiResult.success(result);
                }
            }

            // 2. 获取美食ID列表
            List<Integer> gourmetIds = new ArrayList<>();
            for (Object id : trendingItems.keySet()) {
                try {
                    gourmetIds.add(Integer.parseInt(id.toString()));
                } catch (NumberFormatException e) {
                    log.warn("Invalid gourmet ID in trending foods: {}", id);
                }
            }

            // 3. 查询美食详情
            if (!gourmetIds.isEmpty()) {
                List<GourmetVO> gourmetList = gourmetMapper.queryByIds(gourmetIds);

                // 4. 按照排行榜顺序排序
                Map<Integer, GourmetVO> gourmetMap = gourmetList.stream()
                        .collect(Collectors.toMap(GourmetVO::getId, vo -> vo));

                List<GourmetVO> sortedResult = new ArrayList<>();
                for (Integer id : gourmetIds) {
                    GourmetVO vo = gourmetMap.get(id);
                    if (vo != null) {
                        sortedResult.add(vo);
                    }
                }

                return ApiResult.success(sortedResult);
            }

            return ApiResult.success(Collections.emptyList());
        } catch (Exception e) {
            log.error("Failed to get trending foods: {}", e.getMessage(), e);
            return ApiResult.error("获取热门美食失败");
        }
    }

    /**
     * 记录美食UV
     * 
     * @param gourmetId 美食ID
     * @param userId    用户ID
     * @return 操作结果
     */
    @Override
    public Result<Void> recordGourmetUV(Integer gourmetId, Integer userId) {
        if (gourmetId == null) {
            return ApiResult.error("美食ID不能为空");
        }

        if (userId == null) {
            userId = LocalThreadHolder.getUserId();
            if (userId == null) {
                return ApiResult.error("用户未登录");
            }
        }

        try {
            // 1. 生成当天的日期字符串
            String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

            // 2. 构建UV统计键
            String uvKey = CacheConstants.GOURMET_UV_KEY_PREFIX + gourmetId + ":" + today;

            // 3. 使用HyperLogLog记录UV
            redisUtil.pfAdd(uvKey, userId);

            // 4. 设置过期时间
            redisUtil.expire(uvKey, CacheConstants.GOURMET_UV_EXPIRE);

            // 5. 更新热门排行榜
            redisUtil.zAdd(CacheConstants.TRENDING_FOODS_KEY, gourmetId, 1);
            redisUtil.expire(CacheConstants.TRENDING_FOODS_KEY, CacheConstants.TRENDING_FOODS_EXPIRE);

            return ApiResult.success();
        } catch (Exception e) {
            log.error("Failed to record gourmet UV: {}", e.getMessage(), e);
            return ApiResult.error("记录UV失败");
        }
    }

    /**
     * 获取美食UV统计
     * 
     * @param gourmetId 美食ID
     * @return UV数量
     */
    @Override
    public Result<Long> getGourmetUV(Integer gourmetId) {
        if (gourmetId == null) {
            return ApiResult.error("美食ID不能为空");
        }

        try {
            // 1. 生成当天的日期字符串
            String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

            // 2. 构建UV统计键
            String uvKey = CacheConstants.GOURMET_UV_KEY_PREFIX + gourmetId + ":" + today;

            // 3. 获取UV统计
            long uv = redisUtil.pfCount(uvKey);

            return ApiResult.success(uv);
        } catch (Exception e) {
            log.error("Failed to get gourmet UV: {}", e.getMessage(), e);
            return ApiResult.error("获取UV失败");
        }
    }

    /**
     * 更新热门排行榜
     * 同步数据库中的浏览量数据到Redis排行榜
     * 
     * @return 操作结果
     */
    @Override
    public Result<Void> updateTrendingFoods() {
        try {
            // 1. 查询数据库中的热门美食
            GourmetQueryDto queryDto = new GourmetQueryDto();
            queryDto.setCurrent(0);
            queryDto.setSize(100); // 获取前100条热门美食
            List<GourmetVO> hotGourmets = gourmetMapper.queryByView(queryDto);

            if (CollectionUtils.isEmpty(hotGourmets)) {
                return ApiResult.success();
            }

            // 2. 构建排行榜数据
            Map<Object, Double> scoreMap = new HashMap<>();
            for (GourmetVO gourmet : hotGourmets) {
                if (gourmet.getViewCount() != null && gourmet.getViewCount() > 0) {
                    scoreMap.put(gourmet.getId(), (double) gourmet.getViewCount());
                }
            }

            // 3. 更新Redis排行榜
            if (!scoreMap.isEmpty()) {
                // 先删除旧的排行榜
                redisUtil.del(CacheConstants.TRENDING_FOODS_KEY);
                // 添加新的排行数据
                redisUtil.zAddAll(CacheConstants.TRENDING_FOODS_KEY, scoreMap);
                // 设置过期时间
                redisUtil.expire(CacheConstants.TRENDING_FOODS_KEY, CacheConstants.TRENDING_FOODS_EXPIRE);
            }

            return ApiResult.success();
        } catch (Exception e) {
            log.error("Failed to update trending foods: {}", e.getMessage(), e);
            return ApiResult.error("更新热门排行榜失败");
        }
    }
}
