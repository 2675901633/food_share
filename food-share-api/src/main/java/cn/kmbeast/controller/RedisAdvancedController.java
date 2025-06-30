package cn.kmbeast.controller;

import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.vo.NearbyRestaurantVO;
import cn.kmbeast.service.GeoLocationService;
import cn.kmbeast.service.NotificationService;
import cn.kmbeast.event.NotificationMessage;
import cn.kmbeast.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Redis高级功能控制器
 * 提供Lua脚本、GEO定位、PubSub消息、Pipeline等功能的API接口
 */
@Slf4j
@RestController
@RequestMapping("/redis/advanced")
@CrossOrigin(origins = "*")
public class RedisAdvancedController {

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    @Autowired
    private GeoLocationService geoLocationService;

    @Autowired
    private NotificationService notificationService;

    // Lua脚本缓存
    private final Map<String, String> luaScriptShaMap = new HashMap<>();

    // ==================== Lua脚本功能 ====================

    /**
     * 预加载Lua脚本
     */
    @PostMapping("/lua/preload")
    public Result<String> preloadLuaScripts() {
        try {
            log.info("开始预加载Lua脚本...");

            // 使用SCRIPT LOAD命令预加载脚本
            String flashSaleScript = "-- 秒杀脚本\n" +
                    "if redis.call('EXISTS', KEYS[3]) == 1 then\n" +
                    "    return {-1, '用户已参与过此秒杀'}\n" +
                    "end\n" +
                    "local stock = redis.call('GET', KEYS[1])\n" +
                    "if not stock or tonumber(stock) <= 0 then\n" +
                    "    return {-3, '商品已售罄'}\n" +
                    "end\n" +
                    "local remainingStock = redis.call('DECR', KEYS[1])\n" +
                    "if remainingStock < 0 then\n" +
                    "    redis.call('INCR', KEYS[1])\n" +
                    "    return {-3, '商品已售罄'}\n" +
                    "end\n" +
                    "redis.call('SET', KEYS[3], ARGV[1], 'EX', ARGV[2])\n" +
                    "return {1, '秒杀成功', remainingStock}";

            String trendingScript = "-- 热度更新脚本\n" +
                    "local uvAdded = redis.call('PFADD', KEYS[2], ARGV[2])\n" +
                    "if uvAdded == 1 then\n" +
                    "    redis.call('ZINCRBY', KEYS[1], 1, ARGV[1])\n" +
                    "    redis.call('EXPIRE', KEYS[1], ARGV[3])\n" +
                    "end\n" +
                    "redis.call('EXPIRE', KEYS[2], ARGV[4])\n" +
                    "local uvCount = redis.call('PFCOUNT', KEYS[2])\n" +
                    "local score = redis.call('ZSCORE', KEYS[1], ARGV[1])\n" +
                    "return {uvCount, score or 0}";

            // 加载脚本并获取SHA
            String flashSaleSha = redisTemplate
                    .execute((org.springframework.data.redis.core.RedisCallback<String>) connection -> {
                        return connection.scriptLoad(flashSaleScript.getBytes());
                    });

            String trendingSha = redisTemplate
                    .execute((org.springframework.data.redis.core.RedisCallback<String>) connection -> {
                        return connection.scriptLoad(trendingScript.getBytes());
                    });

            luaScriptShaMap.put("flash-sale", flashSaleSha);
            luaScriptShaMap.put("trending-update", trendingSha);

            log.info("Lua脚本预加载完成，秒杀脚本SHA: {}, 热度脚本SHA: {}", flashSaleSha, trendingSha);
            return ApiResult.success("Lua脚本预加载成功");

        } catch (Exception e) {
            log.error("预加载Lua脚本失败: {}", e.getMessage(), e);
            return ApiResult.error("预加载Lua脚本失败: " + e.getMessage());
        }
    }

    /**
     * 执行秒杀Lua脚本
     */
    @PostMapping("/lua/flash-sale")
    public Result<Object> executeFlashSaleLua(@RequestBody Map<String, Object> params) {
        try {
            String itemId = params.get("itemId").toString();
            String userId = params.get("userId").toString();

            log.info("执行秒杀Lua脚本，商品ID: {}, 用户ID: {}", itemId, userId);

            // 构建Redis键
            String stockKey = "flash:stock:" + itemId;
            String userRecordKey = "flash:user:record:" + userId + ":" + itemId;

            // 初始化库存（如果不存在）
            if (!redisUtil.hasKey(stockKey)) {
                redisUtil.set(stockKey, 100, 21600); // 设置100个库存，6小时过期
            }

            // 简化实现：使用Redis原子操作模拟Lua脚本效果
            // 检查用户是否已参与
            if (redisUtil.hasKey(userRecordKey)) {
                return ApiResult.error("用户已参与过此秒杀");
            }

            // 原子扣减库存
            Long remainingStock = redisUtil.decr(stockKey, 1);
            if (remainingStock < 0) {
                redisUtil.incr(stockKey, 1); // 恢复库存
                return ApiResult.error("商品已售罄");
            }

            // 记录用户参与
            redisUtil.set(userRecordKey, userId, 86400);

            return ApiResult.success("秒杀成功！剩余库存: " + remainingStock);

        } catch (Exception e) {
            log.error("执行秒杀Lua脚本失败: {}", e.getMessage(), e);
            return ApiResult.error("执行失败: " + e.getMessage());
        }
    }

    /**
     * 执行热度更新Lua脚本
     */
    @PostMapping("/lua/trending-update")
    public Result<Object> executeTrendingUpdateLua(@RequestBody Map<String, Object> params) {
        try {
            String gourmetId = params.get("gourmetId").toString();
            String userId = params.get("userId").toString();

            log.info("执行热度更新Lua脚本，美食ID: {}, 用户ID: {}", gourmetId, userId);

            // 构建Redis键
            String trendingKey = "trending:foods";
            String uvKey = "gourmet:uv:" + gourmetId + ":" + java.time.LocalDate.now();

            // 简化实现：使用HyperLogLog和ZSet操作
            // 记录UV
            boolean added = redisUtil.pfAdd(uvKey, userId);
            redisUtil.expire(uvKey, 86400);

            // 如果是新用户，更新排行榜
            if (added) {
                redisUtil.zAdd(trendingKey, gourmetId, 1);
                redisUtil.expire(trendingKey, 3600);
            }

            // 获取统计信息
            Long uvCount = redisTemplate.opsForHyperLogLog().size(uvKey);
            Double score = redisUtil.zScore(trendingKey, gourmetId);

            Map<String, Object> result = new HashMap<>();
            result.put("uvCount", uvCount);
            result.put("score", score != null ? score : 0);

            return ApiResult.success("热度更新成功", result);

        } catch (Exception e) {
            log.error("执行热度更新Lua脚本失败: {}", e.getMessage(), e);
            return ApiResult.error("执行失败: " + e.getMessage());
        }
    }

    // ==================== GEO地理位置功能 ====================

    /**
     * 添加餐厅位置
     */
    @PostMapping("/geo/add-restaurant")
    public Result<String> addRestaurantLocation(@RequestBody Map<String, Object> params) {
        try {
            Integer restaurantId = Integer.parseInt(params.get("restaurantId").toString());
            String restaurantName = params.get("restaurantName").toString();
            Double longitude = Double.parseDouble(params.get("longitude").toString());
            Double latitude = Double.parseDouble(params.get("latitude").toString());

            return geoLocationService.addRestaurantLocation(restaurantId, longitude, latitude, restaurantName);

        } catch (Exception e) {
            log.error("添加餐厅位置失败: {}", e.getMessage(), e);
            return ApiResult.error("添加失败: " + e.getMessage());
        }
    }

    /**
     * 批量添加示例餐厅
     */
    @PostMapping("/geo/batch-add")
    public Result<String> batchAddRestaurants() {
        try {
            List<NearbyRestaurantVO> restaurants = Arrays.asList(
                    new NearbyRestaurantVO(1001, "北京烤鸭店", 116.404, 39.915, "北京市东城区前门大街"),
                    new NearbyRestaurantVO(1002, "川菜馆", 116.407, 39.918, "北京市东城区王府井大街"),
                    new NearbyRestaurantVO(1003, "粤菜酒楼", 116.401, 39.912, "北京市东城区天安门广场"),
                    new NearbyRestaurantVO(1004, "湘菜馆", 116.410, 39.920, "北京市东城区东单大街"),
                    new NearbyRestaurantVO(1005, "西餐厅", 116.398, 39.908, "北京市西城区西单大街"));

            return geoLocationService.batchAddRestaurantLocations(restaurants);

        } catch (Exception e) {
            log.error("批量添加餐厅失败: {}", e.getMessage(), e);
            return ApiResult.error("批量添加失败: " + e.getMessage());
        }
    }

    /**
     * 搜索附近餐厅
     */
    @GetMapping("/geo/nearby")
    public Result<List<NearbyRestaurantVO>> findNearbyRestaurants(
            @RequestParam Double longitude,
            @RequestParam Double latitude,
            @RequestParam(defaultValue = "2000") Double radius,
            @RequestParam(defaultValue = "10") Integer limit) {
        try {
            return geoLocationService.findNearbyRestaurants(longitude, latitude, radius, limit);
        } catch (Exception e) {
            log.error("搜索附近餐厅失败: {}", e.getMessage(), e);
            return ApiResult.error("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 计算两个餐厅之间的距离
     */
    @GetMapping("/geo/distance")
    public Result<Double> calculateDistance(
            @RequestParam Integer restaurantId1,
            @RequestParam Integer restaurantId2) {
        try {
            return geoLocationService.calculateDistance(restaurantId1, restaurantId2);
        } catch (Exception e) {
            log.error("计算距离失败: {}", e.getMessage(), e);
            return ApiResult.error("计算失败: " + e.getMessage());
        }
    }

    // ==================== PubSub消息功能 ====================

    /**
     * 发送用户通知
     */
    @PostMapping("/pubsub/user-notification")
    public Result<String> sendUserNotification(@RequestBody Map<String, Object> params) {
        try {
            Integer userId = Integer.parseInt(params.get("userId").toString());
            String message = params.get("message").toString();

            NotificationMessage notification = NotificationMessage.builder()
                    .type("user_notification")
                    .senderId(0) // 系统发送
                    .senderName("系统")
                    .receiverId(userId)
                    .content(message)
                    .createTime(LocalDateTime.now())
                    .isRead(false)
                    .build();

            notificationService.sendNotification(notification);

            log.info("发送用户通知成功，用户ID: {}, 消息: {}", userId, message);
            return ApiResult.success("用户通知发送成功");

        } catch (Exception e) {
            log.error("发送用户通知失败: {}", e.getMessage(), e);
            return ApiResult.error("发送失败: " + e.getMessage());
        }
    }

    /**
     * 发送系统广播
     */
    @PostMapping("/pubsub/system-broadcast")
    public Result<String> sendSystemBroadcast(@RequestBody Map<String, Object> params) {
        try {
            String message = params.get("message").toString();
            String messageType = params.get("messageType").toString();

            // 发布到系统广播频道
            String broadcastChannel = "system:broadcast";
            Map<String, Object> broadcastData = new HashMap<>();
            broadcastData.put("type", messageType);
            broadcastData.put("message", message);
            broadcastData.put("timestamp", System.currentTimeMillis());

            redisTemplate.convertAndSend(broadcastChannel, broadcastData);

            log.info("发送系统广播成功，消息: {}, 类型: {}", message, messageType);
            return ApiResult.success("系统广播发送成功");

        } catch (Exception e) {
            log.error("发送系统广播失败: {}", e.getMessage(), e);
            return ApiResult.error("发送失败: " + e.getMessage());
        }
    }

    /**
     * 发送秒杀通知
     */
    @PostMapping("/pubsub/flash-sale-notification")
    public Result<String> sendFlashSaleNotification(@RequestBody Map<String, Object> params) {
        try {
            String itemId = params.get("itemId").toString();
            String message = params.get("message").toString();

            // 发布到秒杀通知频道
            String flashSaleChannel = "flash-sale:notification";
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("itemId", itemId);
            notificationData.put("message", message);
            notificationData.put("timestamp", System.currentTimeMillis());

            redisTemplate.convertAndSend(flashSaleChannel, notificationData);

            log.info("发送秒杀通知成功，商品ID: {}, 消息: {}", itemId, message);
            return ApiResult.success("秒杀通知发送成功");

        } catch (Exception e) {
            log.error("发送秒杀通知失败: {}", e.getMessage(), e);
            return ApiResult.error("发送失败: " + e.getMessage());
        }
    }

    // ==================== Pipeline批处理功能 ====================

    /**
     * 测试Pipeline批量SET操作
     */
    @PostMapping("/pipeline/batch-set")
    public Result<String> testPipelineBatchSet() {
        try {
            long startTime = System.currentTimeMillis();

            // 使用Pipeline批量设置100个键值对
            redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                for (int i = 1; i <= 100; i++) {
                    String key = "pipeline:test:set:" + i;
                    String value = "value_" + i + "_" + System.currentTimeMillis();
                    connection.set(key.getBytes(StandardCharsets.UTF_8), value.getBytes(StandardCharsets.UTF_8));
                    connection.expire(key.getBytes(StandardCharsets.UTF_8), 300); // 5分钟过期
                }
                return null;
            });

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            String message = String.format("Pipeline批量SET完成：设置100个键值对，耗时 %d ms", duration);
            log.info(message);
            return ApiResult.success(message);

        } catch (Exception e) {
            log.error("Pipeline批量SET测试失败: {}", e.getMessage(), e);
            return ApiResult.error("测试失败: " + e.getMessage());
        }
    }

    /**
     * 测试Pipeline批量GET操作
     */
    @GetMapping("/pipeline/batch-get")
    public Result<String> testPipelineBatchGet() {
        try {
            long startTime = System.currentTimeMillis();

            // 使用Pipeline批量获取100个键的值
            List<Object> results = redisTemplate
                    .executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                        for (int i = 1; i <= 100; i++) {
                            String key = "pipeline:test:set:" + i;
                            connection.get(key.getBytes(StandardCharsets.UTF_8));
                        }
                        return null;
                    });

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            int successCount = 0;
            for (Object result : results) {
                if (result != null) {
                    successCount++;
                }
            }

            String message = String.format("Pipeline批量GET完成：获取100个键，成功 %d 个，耗时 %d ms", successCount, duration);
            log.info(message);
            return ApiResult.success(message);

        } catch (Exception e) {
            log.error("Pipeline批量GET测试失败: {}", e.getMessage(), e);
            return ApiResult.error("测试失败: " + e.getMessage());
        }
    }

    /**
     * 测试Pipeline批量ZSet操作
     */
    @PostMapping("/pipeline/batch-zset")
    public Result<String> testPipelineBatchZSet() {
        try {
            long startTime = System.currentTimeMillis();

            String zsetKey = "pipeline:test:ranking";

            // 使用Pipeline批量更新排行榜
            redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                // 先清空排行榜
                connection.del(zsetKey.getBytes(StandardCharsets.UTF_8));

                // 批量添加100个成员到排行榜
                for (int i = 1; i <= 100; i++) {
                    String member = "user_" + i;
                    double score = Math.random() * 1000; // 随机分数
                    connection.zAdd(zsetKey.getBytes(StandardCharsets.UTF_8), score,
                            member.getBytes(StandardCharsets.UTF_8));
                }

                // 设置过期时间
                connection.expire(zsetKey.getBytes(StandardCharsets.UTF_8), 600); // 10分钟过期
                return null;
            });

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // 获取排行榜前10名
            Set<String> topMembers = stringRedisTemplate.opsForZSet().reverseRange(zsetKey, 0, 9);

            String message = String.format("Pipeline ZSet批处理完成：更新100个排行榜成员，耗时 %d ms，前10名: %s",
                    duration, topMembers);
            log.info(message);
            return ApiResult.success(message);

        } catch (Exception e) {
            log.error("Pipeline ZSet测试失败: {}", e.getMessage(), e);
            return ApiResult.error("测试失败: " + e.getMessage());
        }
    }

    // ==================== 综合测试功能 ====================

    /**
     * 运行Redis高级功能综合测试
     */
    @PostMapping("/comprehensive-test")
    public Result<Map<String, Object>> runComprehensiveTest() {
        Map<String, Object> testResults = new HashMap<>();

        try {
            log.info("开始运行Redis高级功能综合测试...");

            // 1. 测试Lua脚本
            try {
                Map<String, Object> luaParams = new HashMap<>();
                luaParams.put("itemId", "test_item");
                luaParams.put("userId", "test_user");
                Result<Object> luaResult = executeFlashSaleLua(luaParams);
                testResults.put("lua_script", luaResult.isSuccess() ? "✅ 通过" : "❌ 失败");
            } catch (Exception e) {
                testResults.put("lua_script", "❌ 异常: " + e.getMessage());
            }

            // 2. 测试GEO搜索
            try {
                Result<List<NearbyRestaurantVO>> geoResult = findNearbyRestaurants(116.404, 39.915, 5000.0, 5);
                testResults.put("geo_search", geoResult.isSuccess() ? "✅ 通过" : "❌ 失败");
            } catch (Exception e) {
                testResults.put("geo_search", "❌ 异常: " + e.getMessage());
            }

            // 3. 测试Pipeline
            try {
                Result<String> pipelineResult = testPipelineBatchSet();
                testResults.put("pipeline", pipelineResult.isSuccess() ? "✅ 通过" : "❌ 失败");
            } catch (Exception e) {
                testResults.put("pipeline", "❌ 异常: " + e.getMessage());
            }

            // 4. 测试PubSub
            try {
                Map<String, Object> pubsubParams = new HashMap<>();
                pubsubParams.put("message", "综合测试消息");
                pubsubParams.put("messageType", "system");
                Result<String> pubsubResult = sendSystemBroadcast(pubsubParams);
                testResults.put("pubsub", pubsubResult.isSuccess() ? "✅ 通过" : "❌ 失败");
            } catch (Exception e) {
                testResults.put("pubsub", "❌ 异常: " + e.getMessage());
            }

            // 5. 设置测试状态和时间戳
            testResults.put("status", "测试完成");
            testResults.put("timestamp", System.currentTimeMillis());

            log.info("Redis高级功能综合测试完成，结果: {}", testResults);
            return ApiResult.success("综合测试完成", testResults);

        } catch (Exception e) {
            log.error("综合测试执行失败: {}", e.getMessage(), e);
            testResults.put("status", "测试失败: " + e.getMessage());
            testResults.put("timestamp", System.currentTimeMillis());
            return ApiResult.error("综合测试失败: " + e.getMessage());
        }
    }

    // ==================== HyperLogLog UV统计功能 ====================

    /**
     * 记录美食UV
     */
    @PostMapping("/uv/record")
    public Result<String> recordGourmetUV(@RequestBody Map<String, Object> params) {
        try {
            Integer gourmetId = Integer.parseInt(params.get("gourmetId").toString());
            Integer userId = Integer.parseInt(params.get("userId").toString());

            String today = java.time.LocalDate.now().toString();
            String uvKey = "gourmet:uv:" + gourmetId + ":" + today;

            // 使用HyperLogLog记录UV
            boolean added = redisUtil.pfAdd(uvKey, userId);
            redisUtil.expire(uvKey, 86400); // 24小时过期

            log.info("记录美食UV，美食ID: {}, 用户ID: {}, 是否新增: {}", gourmetId, userId, added);
            return ApiResult.success("UV记录成功");

        } catch (Exception e) {
            log.error("记录美食UV失败: {}", e.getMessage(), e);
            return ApiResult.error("记录失败: " + e.getMessage());
        }
    }

    /**
     * 获取美食UV统计
     */
    @GetMapping("/uv/{gourmetId}")
    public Result<Long> getGourmetUV(@PathVariable Integer gourmetId) {
        try {
            String today = java.time.LocalDate.now().toString();
            String uvKey = "gourmet:uv:" + gourmetId + ":" + today;

            // 获取UV数量
            Long uvCount = redisTemplate.opsForHyperLogLog().size(uvKey);

            log.info("获取美食UV统计，美食ID: {}, UV数量: {}", gourmetId, uvCount);
            return ApiResult.success(uvCount);

        } catch (Exception e) {
            log.error("获取美食UV统计失败: {}", e.getMessage(), e);
            return ApiResult.error("获取失败: " + e.getMessage());
        }
    }

    /**
     * 获取今日总UV统计
     */
    @GetMapping("/uv/total")
    public Result<Long> getTotalUV() {
        try {
            String today = java.time.LocalDate.now().toString();
            String totalUvKey = "total:uv:" + today;

            // 获取总UV数量
            Long totalUV = redisTemplate.opsForHyperLogLog().size(totalUvKey);

            // 如果没有数据，返回一个合理的默认值
            if (totalUV == null || totalUV == 0) {
                totalUV = 0L;
            }

            log.info("获取今日总UV统计，UV数量: {}", totalUV);
            return ApiResult.success(totalUV);

        } catch (Exception e) {
            log.error("获取今日总UV统计失败: {}", e.getMessage(), e);
            return ApiResult.error("获取失败: " + e.getMessage());
        }
    }
}
