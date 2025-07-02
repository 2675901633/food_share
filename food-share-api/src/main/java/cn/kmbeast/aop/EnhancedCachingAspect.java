package cn.kmbeast.aop;

import cn.kmbeast.utils.ApiResult;
import cn.kmbeast.utils.CacheConstants;
import cn.kmbeast.utils.RedisUtil;
import cn.kmbeast.utils.Result;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Random;

/**
 * 增强版缓存切面
 * 解决缓存穿透、缓存雪崩问题
 */
@Slf4j
@Aspect
@Component
public class EnhancedCachingAspect {

    @Autowired
    private RedisUtil redisUtil;

    private static final String NULL_CACHE_VALUE = "NULL_CACHE_PLACEHOLDER";
    private static final long NULL_CACHE_EXPIRE = 300; // 空值缓存5分钟
    private static final Random random = new Random();

    @Pointcut("@annotation(cn.kmbeast.aop.CacheableGourmet)")
    public void gourmetCachePointcut() {}

    /**
     * 增强版缓存环绕通知
     * 1. 解决缓存穿透：缓存空值
     * 2. 解决缓存雪崩：随机化过期时间
     */
    @Around("gourmetCachePointcut()")
    public Object enhancedCacheAround(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        CacheableGourmet cacheableGourmet = method.getAnnotation(CacheableGourmet.class);
        Class<?> returnType = method.getReturnType();

        // 获取缓存键
        String cacheKey = getCacheKey(joinPoint, cacheableGourmet);
        log.debug("增强缓存键: {}", cacheKey);

        // 从缓存获取数据
        Object cacheValue = redisUtil.get(cacheKey);
        if (cacheValue != null) {
            // 检查是否为空值缓存
            if (NULL_CACHE_VALUE.equals(cacheValue.toString())) {
                log.debug("命中空值缓存: {}", cacheKey);
                return createEmptyResult();
            }
            
            log.debug("从缓存读取数据: {}", cacheKey);
            try {
                return JSON.parseObject(cacheValue.toString(), returnType);
            } catch (Exception e) {
                log.error("缓存数据反序列化失败，删除缓存: {}", e.getMessage());
                redisUtil.del(cacheKey);
            }
        }

        // 缓存未命中，执行原方法
        log.debug("缓存未命中，执行原方法: {}", cacheKey);
        Object result = joinPoint.proceed();

        // 缓存结果（包括空值）
        if (result != null && result instanceof Result<?>) {
            Result<?> apiResult = (Result<?>) result;
            
            if (apiResult.getData() != null && !isEmpty(apiResult.getData())) {
                // 正常结果缓存，使用随机化过期时间
                long expire = getRandomizedExpire(cacheableGourmet.expire());
                String jsonResult = JSON.toJSONString(result);
                redisUtil.set(cacheKey, jsonResult, expire);
                log.debug("缓存正常数据: {}, 过期时间: {}s", cacheKey, expire);
            } else {
                // 空值缓存，设置较短过期时间
                redisUtil.set(cacheKey, NULL_CACHE_VALUE, NULL_CACHE_EXPIRE);
                log.debug("缓存空值数据: {}, 过期时间: {}s", cacheKey, NULL_CACHE_EXPIRE);
            }
        }

        return result;
    }

    /**
     * 生成随机化过期时间，防止缓存雪崩
     */
    private long getRandomizedExpire(long baseExpire) {
        // 在基础过期时间上增加0-20%的随机时间
        long randomOffset = (long) (baseExpire * 0.2 * random.nextDouble());
        return baseExpire + randomOffset;
    }

    /**
     * 创建空结果
     */
    private Result<?> createEmptyResult() {
        return ApiResult.success("数据不存在", null);
    }

    /**
     * 判断数据是否为空
     */
    private boolean isEmpty(Object data) {
        if (data == null) return true;
        if (data instanceof java.util.Collection) {
            return ((java.util.Collection<?>) data).isEmpty();
        }
        return false;
    }

    /**
     * 生成缓存键
     */
    private String getCacheKey(ProceedingJoinPoint joinPoint, CacheableGourmet cacheableGourmet) {
        // 复用原有的缓存键生成逻辑
        Object[] args = joinPoint.getArgs();
        String methodName = joinPoint.getSignature().getName();
        
        if (cacheableGourmet.type() == CacheType.SINGLE && args.length > 0) {
            return CacheConstants.GOURMET_DETAIL_KEY_PREFIX + args[0];
        } else {
            return CacheConstants.GOURMET_LIST_KEY + ":" + methodName;
        }
    }
} 