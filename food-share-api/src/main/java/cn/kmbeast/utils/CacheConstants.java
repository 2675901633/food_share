package cn.kmbeast.utils;

import java.util.Random;

/**
 * 缓存常量类，定义缓存键和过期时间
 * 针对集群模式优化key设计
 */
public class CacheConstants {

    // ==================== 基础业务缓存 ====================

    /**
     * 美食详情缓存前缀
     * 使用hash tag确保相关key在同一slot
     */
    public static final String GOURMET_DETAIL_KEY_PREFIX = "gourmet:detail:";

    /**
     * 美食列表缓存键
     */
    public static final String GOURMET_LIST_KEY = "gourmet:list";

    /**
     * 热门美食缓存键
     */
    public static final String GOURMET_HOT_KEY = "gourmet:hot";

    /**
     * 分类列表缓存键
     */
    public static final String CATEGORY_LIST_KEY = "category:list";

    /**
     * 用户信息缓存前缀
     * 使用hash tag确保用户相关的key在同一个slot
     */
    public static final String USER_INFO_KEY_PREFIX = "user:{info}:";

    /**
     * 推荐美食缓存前缀
     */
    public static final String RECOMMEND_GOURMET_KEY_PREFIX = "recommend:gourmet:";

    /**
     * 相似内容推荐缓存前缀
     */
    public static final String SIMILAR_CONTENT_KEY_PREFIX = "recommend:similar:";

    /**
     * 内容特征缓存前缀
     */
    public static final String CONTENT_FEATURES_KEY_PREFIX = "content:features:";

    /**
     * 美食访问量缓存前缀
     */
    public static final String GOURMET_VIEW_COUNT_KEY_PREFIX = "gourmet:view:";

    /**
     * 用户浏览历史缓存前缀
     * 使用hash tag确保用户相关的key在同一个slot
     */
    public static final String USER_HISTORY_KEY_PREFIX = "user:{history}:";

    /**
     * 用户偏好缓存前缀
     * 使用hash tag确保用户相关的key在同一个slot
     */
    public static final String USER_PREFERENCES_KEY_PREFIX = "user:{preferences}:";

    /**
     * 用户最近浏览缓存前缀
     * 使用hash tag确保用户相关的key在同一个slot
     */
    public static final String USER_RECENT_VIEW_KEY_PREFIX = "user:{recent}:";

    /**
     * 用户个性化推荐缓存前缀
     * 使用hash tag确保用户相关的key在同一个slot
     */
    public static final String USER_RECOMMENDATIONS_KEY_PREFIX = "user:{recommendations}:";

    /**
     * 热门美食排行榜缓存键
     */
    public static final String TRENDING_FOODS_KEY = "trending:foods";

    /**
     * 美食UV统计前缀
     */
    public static final String GOURMET_UV_KEY_PREFIX = "gourmet:uv:";

    // ==================== 秒杀相关缓存 ====================

    /**
     * 秒杀商品库存缓存前缀
     * 使用hash tag确保秒杀相关的key在同一个slot
     */
    public static final String FLASH_SALE_STOCK_PREFIX = "flash:{stock}:";

    /**
     * 秒杀商品信息缓存前缀
     * 使用hash tag确保秒杀相关的key在同一个slot
     */
    public static final String FLASH_SALE_ITEM_PREFIX = "flash:{item}:";

    /**
     * 秒杀商品列表缓存键
     */
    public static final String FLASH_SALE_ITEM_LIST = "flash:item:list";

    /**
     * 秒杀分布式锁前缀
     * 使用hash tag确保秒杀相关的key在同一个slot
     */
    public static final String FLASH_SALE_LOCK_PREFIX = "flash:{lock}:";

    /**
     * 秒杀接口访问限流前缀
     * 使用hash tag确保秒杀相关的key在同一个slot
     */
    public static final String FLASH_SALE_RATE_LIMIT_PREFIX = "flash:{limit}:";

    /**
     * 秒杀订单缓存前缀
     * 使用hash tag确保秒杀相关的key在同一个slot
     */
    public static final String FLASH_SALE_ORDER_PREFIX = "flash:{order}:";

    /**
     * 用户秒杀记录前缀（防止重复下单）
     * 使用hash tag确保秒杀相关的key在同一个slot
     */
    public static final String FLASH_SALE_USER_RECORD_PREFIX = "flash:{record}:";

    // ==================== 过期时间设置 ====================

    /**
     * 美食详情缓存过期时间（秒）- 2小时
     */
    public static final long GOURMET_DETAIL_EXPIRE = 7200;

    /**
     * 美食列表缓存过期时间（秒）- 30分钟
     */
    public static final long GOURMET_LIST_EXPIRE = 1800;

    /**
     * 热门美食缓存过期时间（秒）- 10分钟
     */
    public static final long GOURMET_HOT_EXPIRE = 600;

    /**
     * 分类缓存过期时间（秒）- 1天
     */
    public static final long CATEGORY_EXPIRE = 86400;

    /**
     * 用户信息缓存过期时间（秒）- 1小时
     */
    public static final long USER_INFO_EXPIRE = 3600;

    /**
     * 推荐美食缓存过期时间（秒）- 15分钟
     */
    public static final long RECOMMEND_EXPIRE = 900;

    /**
     * 相似内容推荐缓存过期时间（秒）- 30分钟
     */
    public static final long SIMILAR_CONTENT_EXPIRE = 1800;

    /**
     * 内容特征缓存过期时间（秒）- 1天
     */
    public static final long CONTENT_FEATURES_EXPIRE = 86400;

    /**
     * 用户浏览历史缓存过期时间（秒）- 30天
     */
    public static final long USER_HISTORY_EXPIRE = 2592000;

    /**
     * 用户偏好缓存过期时间（秒）- 7天
     */
    public static final long USER_PREFERENCES_EXPIRE = 604800;

    /**
     * 用户最近浏览缓存过期时间（秒）- 1天
     */
    public static final long USER_RECENT_VIEW_EXPIRE = 86400;

    /**
     * 用户个性化推荐缓存过期时间（秒）- 1小时
     */
    public static final long USER_RECOMMENDATIONS_EXPIRE = 3600;

    /**
     * 热门排行榜缓存过期时间（秒）- 5分钟
     */
    public static final long TRENDING_FOODS_EXPIRE = 300;

    /**
     * UV统计数据过期时间（秒）- 1天
     */
    public static final long GOURMET_UV_EXPIRE = 86400;

    /**
     * 用户浏览历史最大记录数
     */
    public static final int USER_HISTORY_MAX_SIZE = 100;

    /**
     * 用户最近浏览最大记录数
     */
    public static final int USER_RECENT_VIEW_MAX_SIZE = 20;

    /**
     * 秒杀相关常量
     */
    /**
     * 秒杀商品信息缓存过期时间（秒）- 1小时
     */
    public static final long FLASH_SALE_ITEM_EXPIRE = 3600;

    /**
     * 秒杀库存缓存过期时间（秒）- 6小时
     */
    public static final long FLASH_SALE_STOCK_EXPIRE = 21600;

    /**
     * 秒杀分布式锁过期时间（毫秒）- 5秒
     */
    public static final long FLASH_SALE_LOCK_EXPIRE = 5000;

    /**
     * 秒杀接口限流时间窗口（秒）
     */
    public static final int FLASH_SALE_RATE_LIMIT_PERIOD = 1;

    /**
     * 秒杀接口限流次数（每个时间窗口内最大请求数）
     */
    public static final int FLASH_SALE_RATE_LIMIT_COUNT = 10;

    /**
     * 用户秒杀记录过期时间（秒）- 1天
     */
    public static final long FLASH_SALE_USER_RECORD_EXPIRE = 86400;

    /**
     * 秒杀商品信息缓存过期时间（秒）- 1小时
     */
    public static final long FLASH_SALE_ITEM_INFO_EXPIRE = 3600;

    /**
     * 秒杀订单过期时间（秒）- 30分钟，用于支付超时处理
     */
    public static final long FLASH_SALE_ORDER_EXPIRE = 1800;

    // ==================== 新增功能常量 ====================

    /**
     * GEO地理位置相关常量
     */
    public static final String RESTAURANT_GEO_KEY = "restaurants:geo";

    // ==================== PubSub消息频道常量 ====================

    /**
     * 系统广播频道
     */
    public static final String SYSTEM_BROADCAST_CHANNEL = "system:broadcast";

    /**
     * 秒杀通知频道
     */
    public static final String FLASH_SALE_NOTIFICATION_CHANNEL = "flash:sale:notification";

    /**
     * 美食推荐频道前缀
     */
    public static final String GOURMET_RECOMMENDATION_CHANNEL_PREFIX = "gourmet:recommendation:";

    /**
     * 热门排行榜过期时间（秒）- 1小时
     */
    public static final long TRENDING_FOODS_EXPIRE_NEW = 3600;

    /**
     * 获取随机化过期时间，防止缓存雪崩
     * @param baseExpire 基础过期时间
     * @return 随机化后的过期时间
     */
    public static long getRandomizedExpire(long baseExpire) {
        Random random = new Random();
        // 在基础过期时间上增加0-20%的随机时间
        long randomOffset = (long) (baseExpire * 0.2 * random.nextDouble());
        return baseExpire + randomOffset;
    }

    /**
     * 获取随机化的美食详情过期时间
     */
    public static long getRandomizedGourmetDetailExpire() {
        return getRandomizedExpire(GOURMET_DETAIL_EXPIRE);
    }

    /**
     * 获取随机化的美食列表过期时间
     */
    public static long getRandomizedGourmetListExpire() {
        return getRandomizedExpire(GOURMET_LIST_EXPIRE);
    }
}