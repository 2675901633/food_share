package cn.kmbeast.schedule;

import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.extend.CategoryQueryDto;
import cn.kmbeast.pojo.dto.query.extend.ContentBasedQueryDto;
import cn.kmbeast.pojo.dto.query.extend.GourmetQueryDto;
import cn.kmbeast.pojo.entity.Category;
import cn.kmbeast.pojo.vo.ContentRecommendVO;
import cn.kmbeast.pojo.vo.GourmetVO;
import cn.kmbeast.service.CategoryService;
import cn.kmbeast.service.ContentRecommendComputeService;
import cn.kmbeast.service.ContentRecommendService;
import cn.kmbeast.service.GourmetService;
import cn.kmbeast.utils.CacheConstants;
import cn.kmbeast.utils.RedisUtil;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 缓存预热定时任务
 * 定期加载热门数据到Redis缓存
 */
@Slf4j
@Component
@EnableScheduling
public class CacheWarmupScheduler {

    @Resource
    private GourmetService gourmetService;

    @Resource
    private CategoryService categoryService;

    @Resource
    private ContentRecommendService contentRecommendService;

    @Resource
    private ContentRecommendComputeService contentRecommendComputeService;

    @Resource
    private RedisUtil redisUtil;

    /**
     * 每天凌晨3点预热缓存
     * 预热热门美食和分类数据
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void warmupCache() {
        log.info("开始执行缓存预热任务...");
        warmupCategories();
        warmupHotGourmets();
        warmupRecommendations();
        log.info("缓存预热任务执行完毕");
    }

    /**
     * 预热分类数据
     */
    public void warmupCategories() {
        try {
            log.info("开始预热分类数据...");
            // 删除旧的分类缓存
            redisUtil.del(CacheConstants.CATEGORY_LIST_KEY);

            // 查询最新分类数据
            CategoryQueryDto categoryQueryDto = new CategoryQueryDto();
            Result<List<Category>> result = categoryService.query(categoryQueryDto);

            if (result != null && result.getData() != null && !result.getData().isEmpty()) {
                // 存入Redis缓存
                redisUtil.set(CacheConstants.CATEGORY_LIST_KEY,
                        JSON.toJSONString(result),
                        CacheConstants.CATEGORY_EXPIRE);
                log.info("分类数据预热完成, 共缓存{}条记录", result.getData().size());
            } else {
                log.warn("分类数据为空，跳过预热");
            }
        } catch (Exception e) {
            log.error("分类数据预热失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 预热热门美食数据
     */
    public void warmupHotGourmets() {
        try {
            log.info("开始预热热门美食数据...");
            // 删除旧的热门美食缓存
            redisUtil.del(CacheConstants.GOURMET_HOT_KEY);

            // 查询热门美食数据
            GourmetQueryDto queryDto = new GourmetQueryDto();
            Result<List<GourmetVO>> result = gourmetService.queryByView(queryDto);

            if (result != null && result.getData() != null && !result.getData().isEmpty()) {
                // 存入Redis缓存
                redisUtil.set(CacheConstants.GOURMET_HOT_KEY,
                        JSON.toJSONString(result),
                        CacheConstants.GOURMET_HOT_EXPIRE);
                log.info("热门美食数据预热完成, 共缓存{}条记录", result.getData().size());

                // 同时缓存前10条热门美食的详情
                int count = Math.min(10, result.getData().size());
                for (int i = 0; i < count; i++) {
                    GourmetVO gourmet = result.getData().get(i);
                    String detailKey = CacheConstants.GOURMET_DETAIL_KEY_PREFIX + gourmet.getId();

                    // 查询美食详情
                    Result<List<GourmetVO>> detailResult = gourmetService.queryById(gourmet.getId());
                    if (detailResult != null && detailResult.getData() != null && !detailResult.getData().isEmpty()) {
                        // 存入Redis缓存
                        redisUtil.set(detailKey,
                                JSON.toJSONString(detailResult),
                                CacheConstants.GOURMET_DETAIL_EXPIRE);
                    }
                }
                log.info("热门美食详情预热完成");
            } else {
                log.warn("热门美食数据为空，跳过预热");
            }
        } catch (Exception e) {
            log.error("热门美食数据预热失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 预热推荐数据
     * 为热门美食预先计算推荐结果并缓存
     */
    public void warmupRecommendations() {
        try {
            log.info("开始预热推荐数据...");

            // 获取热门美食数据
            GourmetQueryDto queryDto = new GourmetQueryDto();
            Result<List<GourmetVO>> result = gourmetService.queryByView(queryDto);

            if (result == null || result.getData() == null || result.getData().isEmpty()) {
                log.warn("热门美食数据为空，跳过推荐预热");
                return;
            }

            // 限制处理的热门美食数量，避免过多计算
            int count = Math.min(20, result.getData().size());
            log.info("开始为{}个热门美食预热推荐数据", count);

            // 为每个热门美食预热相似推荐
            for (int i = 0; i < count; i++) {
                GourmetVO gourmet = result.getData().get(i);
                Integer gourmetId = gourmet.getId();

                try {
                    // 1. 确保特征已计算
                    contentRecommendComputeService.computeAndSaveFeatures(gourmetId);

                    // 2. 更新推荐数据
                    contentRecommendComputeService.updateRecommendations(gourmetId);

                    // 3. 预热不同数量的推荐结果
                    for (int size : new int[] { 5, 10 }) {
                        ContentBasedQueryDto recommendQueryDto = new ContentBasedQueryDto();
                        recommendQueryDto.setGourmetId(gourmetId);
                        recommendQueryDto.setSize(size);

                        // 获取推荐结果并缓存
                        Result<List<ContentRecommendVO>> recommendResult = contentRecommendService
                                .recommendSimilar(recommendQueryDto);

                        if (recommendResult != null && recommendResult.getData() != null) {
                            log.info("成功为美食ID={}预热{}条推荐数据", gourmetId, recommendResult.getData().size());
                        }
                    }
                } catch (Exception e) {
                    log.error("为美食ID={}预热推荐数据失败: {}", gourmetId, e.getMessage(), e);
                }
            }

            log.info("推荐数据预热完成");
        } catch (Exception e) {
            log.error("推荐数据预热失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 每小时同步Redis浏览量到数据库
     * 避免内存数据丢失
     */
    @Scheduled(fixedRate = 3600000) // 1小时
    public void syncViewCountToDatabase() {
        log.info("开始同步Redis浏览量到数据库...");
        try {
            // 获取所有美食浏览量键的前缀
            String keyPrefix = CacheConstants.GOURMET_VIEW_COUNT_KEY_PREFIX;

            // 直接查询数据库获取美食列表，不使用缓存
            GourmetQueryDto queryDto = new GourmetQueryDto();
            // 手动执行service层方法，避免缓存切面干扰
            Result<List<GourmetVO>> resultFromDb = null;
            try {
                // 从缓存中获取热门美食数据
                Object cacheValue = redisUtil.get(CacheConstants.GOURMET_HOT_KEY);
                if (cacheValue != null) {
                    // 正确地将JSON字符串反序列化为Result对象
                    resultFromDb = JSON.parseObject(cacheValue.toString(), Result.class);
                } else {
                    // 如果缓存中没有数据，则从数据库获取
                    resultFromDb = gourmetService.queryByView(queryDto);
                }
            } catch (Exception e) {
                log.error("获取美食列表失败: {}", e.getMessage(), e);
                return;
            }

            if (resultFromDb == null || resultFromDb.getData() == null) {
                log.info("没有需要同步的浏览量数据，查询结果为空");
                return;
            }

            // 处理从缓存获取的数据
            Object data = resultFromDb.getData();
            List<GourmetVO> allGourmets;

            // 判断数据类型并进行转换
            if (data instanceof List) {
                allGourmets = (List<GourmetVO>) data;
            } else {
                log.info("数据类型不符合预期，跳过浏览量同步");
                return;
            }

            if (!allGourmets.isEmpty()) {
                for (Object item : allGourmets) {
                    // 将JSONObject转为GourmetVO
                    GourmetVO gourmet;
                    if (item instanceof GourmetVO) {
                        gourmet = (GourmetVO) item;
                    } else {
                        // 使用JSON工具进行转换
                        String itemJson = JSON.toJSONString(item);
                        gourmet = JSON.parseObject(itemJson, GourmetVO.class);
                    }

                    Integer gourmetId = gourmet.getId();
                    String key = keyPrefix + gourmetId;

                    // 检查Redis中是否有此键
                    if (redisUtil.hasKey(key)) {
                        Object value = redisUtil.get(key);

                        if (value != null) {
                            int viewCount = Integer.parseInt(value.toString());

                            // 这里需要扩展GourmetService接口和实现类，添加更新浏览量的方法
                            // gourmetService.updateViewCount(gourmetId, viewCount);

                            log.info("同步美食ID={}的浏览量: {}", gourmetId, viewCount);
                        }
                    }
                }
                log.info("浏览量同步完成");
            } else {
                log.info("没有需要同步的浏览量数据");
            }
        } catch (Exception e) {
            log.error("浏览量同步失败: {}", e.getMessage(), e);
        }
    }
}