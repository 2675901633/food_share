package cn.kmbeast.controller;

import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.extend.CategoryQueryDto;
import cn.kmbeast.pojo.dto.query.extend.GourmetQueryDto;
import cn.kmbeast.pojo.entity.Category;
import cn.kmbeast.pojo.vo.GourmetVO;
import cn.kmbeast.schedule.CacheWarmupScheduler;
import cn.kmbeast.service.CategoryService;
import cn.kmbeast.service.GourmetService;
import cn.kmbeast.utils.CacheConstants;
import cn.kmbeast.utils.RedisUtil;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Redis缓存测试控制器
 */
@Slf4j
@RestController
@RequestMapping("/redis-test")
public class RedisCacheTestController {

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private GourmetService gourmetService;

    @Autowired
    private CacheWarmupScheduler cacheWarmupScheduler;

    /**
     * 测试Redis连接
     */
    @GetMapping("/connection")
    public Result<String> testConnection() {
        try {
            log.info("开始测试Redis连接...");
            String key = "test:connection";
            String value = "Redis连接测试成功：" + System.currentTimeMillis();

            boolean result = redisUtil.set(key, value);
            log.info("Redis写入结果: {}", result);

            Object savedValue = redisUtil.get(key);
            log.info("Redis读取结果: {}", savedValue);

            if (savedValue != null && savedValue.toString().equals(value)) {
                log.info("Redis连接测试成功！");
                return ApiResult.success("Redis连接测试成功！读写正常");
            } else {
                log.error("Redis连接测试失败：写入和读取的值不一致");
                return ApiResult.fail("Redis连接测试失败：写入和读取的值不一致");
            }
        } catch (Exception e) {
            log.error("Redis连接测试异常: {}", e.getMessage(), e);
            return ApiResult.fail("Redis连接测试异常: " + e.getMessage());
        }
    }

    /**
     * 测试Redis配置类
     */
    @GetMapping("/config-test")
    public Result<Map<String, Object>> testRedisConfig() {
        Map<String, Object> result = new HashMap<>();
        try {
            log.info("开始测试Redis配置...");

            // 测试RedisTemplate是否注入成功
            result.put("redisTemplateInjected", redisTemplate != null);
            log.info("RedisTemplate注入状态: {}", redisTemplate != null);

            // 测试RedisTemplate的序列化器
            if (redisTemplate != null) {
                result.put("keySerializerClass", redisTemplate.getKeySerializer().getClass().getName());
                result.put("valueSerializerClass", redisTemplate.getValueSerializer().getClass().getName());
                log.info("RedisTemplate键序列化器: {}", redisTemplate.getKeySerializer().getClass().getName());
                log.info("RedisTemplate值序列化器: {}", redisTemplate.getValueSerializer().getClass().getName());
            }

            return ApiResult.success("Redis配置测试完成", result);
        } catch (Exception e) {
            log.error("Redis配置测试异常: {}", e.getMessage(), e);
            return ApiResult.fail("Redis配置测试异常: " + e.getMessage());
        }
    }

    /**
     * 测试缓存注解
     */
    @GetMapping("/cache-annotation")
    public Result<String> testCacheAnnotation() {
        try {
            log.info("开始测试缓存注解...");

            // 清除已有缓存
            String categoryKey = CacheConstants.CATEGORY_LIST_KEY;
            String gourmetHotKey = CacheConstants.GOURMET_HOT_KEY;

            boolean hasCategoryCache = redisUtil.hasKey(categoryKey);
            boolean hasGourmetHotCache = redisUtil.hasKey(gourmetHotKey);

            log.info("测试前分类缓存状态: {}", hasCategoryCache);
            log.info("测试前热门美食缓存状态: {}", hasGourmetHotCache);

            // 删除已有缓存
            redisUtil.del(categoryKey, gourmetHotKey);

            // 调用带缓存注解的方法
            log.info("调用带@CacheableCategory注解的方法...");
            CategoryQueryDto categoryQueryDto = new CategoryQueryDto();
            Result<List<Category>> categoryResult = categoryService.query(categoryQueryDto);
            log.info("分类查询结果: {}", JSON.toJSONString(categoryResult));

            log.info("调用带@CacheableGourmet注解的方法...");
            GourmetQueryDto gourmetQueryDto = new GourmetQueryDto();
            Result<List<GourmetVO>> gourmetResult = gourmetService.queryByView(gourmetQueryDto);
            log.info("热门美食查询结果: {}", JSON.toJSONString(gourmetResult));

            // 验证是否写入了缓存
            hasCategoryCache = redisUtil.hasKey(categoryKey);
            hasGourmetHotCache = redisUtil.hasKey(gourmetHotKey);

            log.info("测试后分类缓存状态: {}", hasCategoryCache);
            log.info("测试后热门美食缓存状态: {}", hasGourmetHotCache);

            if (hasCategoryCache && hasGourmetHotCache) {
                return ApiResult.success("缓存注解测试成功！缓存已生效");
            } else {
                return ApiResult.fail("缓存注解测试失败！部分缓存未生效");
            }
        } catch (Exception e) {
            log.error("缓存注解测试异常: {}", e.getMessage(), e);
            return ApiResult.fail("缓存注解测试异常: " + e.getMessage());
        }
    }

    /**
     * 测试缓存工具类
     */
    @GetMapping("/redis-util")
    public Result<Map<String, Object>> testRedisUtil() {
        Map<String, Object> result = new HashMap<>();
        try {
            log.info("开始测试Redis工具类...");

            // 测试字符串操作
            String strKey = "test:string:" + System.currentTimeMillis();
            String strValue = "测试字符串值";
            boolean setStrResult = redisUtil.set(strKey, strValue, 600);
            Object getStrResult = redisUtil.get(strKey);
            log.info("字符串设置结果: {}", setStrResult);
            log.info("字符串获取结果: {}", getStrResult);
            result.put("stringTest", setStrResult && strValue.equals(getStrResult));

            // 测试Hash操作
            String hashKey = "test:hash:" + System.currentTimeMillis();
            Map<String, Object> hashValues = new HashMap<>();
            hashValues.put("name", "测试名称");
            hashValues.put("value", 100);
            boolean setHashResult = redisUtil.hmset(hashKey, hashValues, 600);
            Map<Object, Object> getHashResult = redisUtil.hmget(hashKey);
            log.info("Hash设置结果: {}", setHashResult);
            log.info("Hash获取结果: {}", getHashResult);
            result.put("hashTest", setHashResult && getHashResult.size() == 2);

            // 测试List操作
            String listKey = "test:list:" + System.currentTimeMillis();
            boolean setListResult = redisUtil.lSet(listKey, "项目1");
            redisUtil.lSet(listKey, "项目2");
            redisUtil.lSet(listKey, "项目3");
            long listSize = redisUtil.lGetListSize(listKey);
            log.info("List设置结果: {}", setListResult);
            log.info("List大小: {}", listSize);
            result.put("listTest", setListResult && listSize == 3);

            // 测试Set操作
            String setKey = "test:set:" + System.currentTimeMillis();
            long setSetResult = redisUtil.sSet(setKey, "值1", "值2", "值3");
            Set<Object> getSetResult = redisUtil.sGet(setKey);
            log.info("Set设置结果: {}", setSetResult);
            log.info("Set获取结果: {}", getSetResult);
            result.put("setTest", setSetResult == 3 && getSetResult.size() == 3);

            // 测试ZSet操作
            String zsetKey = "test:zset:" + System.currentTimeMillis();
            boolean setZSetResult1 = redisUtil.zAdd(zsetKey, "项目A", 1.0);
            boolean setZSetResult2 = redisUtil.zAdd(zsetKey, "项目B", 2.0);
            boolean setZSetResult3 = redisUtil.zAdd(zsetKey, "项目C", 3.0);
            Set<Object> getZSetResult = redisUtil.zRange(zsetKey, 0, -1);
            log.info("ZSet设置结果: {}, {}, {}", setZSetResult1, setZSetResult2, setZSetResult3);
            log.info("ZSet获取结果: {}", getZSetResult);
            result.put("zsetTest", getZSetResult.size() == 3);

            return ApiResult.success("Redis工具类测试完成", result);
        } catch (Exception e) {
            log.error("Redis工具类测试异常: {}", e.getMessage(), e);
            return ApiResult.fail("Redis工具类测试异常: " + e.getMessage());
        }
    }

    /**
     * 测试缓存预热
     */
    @GetMapping("/warmup")
    public Result<String> testCacheWarmup() {
        try {
            log.info("开始测试缓存预热...");

            // 清除已有缓存
            redisUtil.del(CacheConstants.CATEGORY_LIST_KEY);
            redisUtil.del(CacheConstants.GOURMET_HOT_KEY);

            log.info("已清除现有缓存，正在检查缓存状态...");
            boolean hasCategoryCache = redisUtil.hasKey(CacheConstants.CATEGORY_LIST_KEY);
            boolean hasGourmetHotCache = redisUtil.hasKey(CacheConstants.GOURMET_HOT_KEY);

            log.info("预热前分类缓存状态: {}", hasCategoryCache);
            log.info("预热前热门美食缓存状态: {}", hasGourmetHotCache);

            // 手动调用预热方法
            log.info("手动执行缓存预热...");
            cacheWarmupScheduler.warmupCache();

            // 验证是否已预热
            hasCategoryCache = redisUtil.hasKey(CacheConstants.CATEGORY_LIST_KEY);
            hasGourmetHotCache = redisUtil.hasKey(CacheConstants.GOURMET_HOT_KEY);

            log.info("预热后分类缓存状态: {}", hasCategoryCache);
            log.info("预热后热门美食缓存状态: {}", hasGourmetHotCache);

            StringBuilder resultMsg = new StringBuilder();
            resultMsg.append("分类缓存: ").append(hasCategoryCache ? "已预热" : "未预热").append("\n");
            resultMsg.append("热门美食缓存: ").append(hasGourmetHotCache ? "已预热" : "未预热");

            if (hasCategoryCache || hasGourmetHotCache) {
                return ApiResult.success("缓存预热测试完成，部分缓存已预热", resultMsg.toString());
            } else {
                return ApiResult.fail("缓存预热测试失败，无缓存被预热");
            }
        } catch (Exception e) {
            log.error("缓存预热测试异常: {}", e.getMessage(), e);
            return ApiResult.fail("缓存预热测试异常: " + e.getMessage());
        }
    }
}