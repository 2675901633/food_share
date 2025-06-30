package cn.kmbeast.aop;

import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.utils.CacheConstants;
import cn.kmbeast.utils.RedisUtil;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

/**
 * 缓存切面，拦截自定义缓存注解
 */
@Slf4j
@Aspect
@Component
public class CachingAspect {

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 定义美食缓存切点
     */
    @Pointcut("@annotation(cn.kmbeast.aop.CacheableGourmet)")
    public void gourmetCachePointcut() {
    }

    /**
     * 定义分类缓存切点
     */
    @Pointcut("@annotation(cn.kmbeast.aop.CacheableCategory)")
    public void categoryCachePointcut() {
    }

    /**
     * 美食缓存环绕通知
     *
     * @param joinPoint 连接点
     * @return 原方法返回值
     */
    @Around("gourmetCachePointcut()")
    public Object gourmetCacheAround(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        CacheableGourmet cacheableGourmet = method.getAnnotation(CacheableGourmet.class);

        // 获取返回类型，用于正确反序列化
        Class<?> returnType = method.getReturnType();

        // 根据缓存类型获取缓存键
        String cacheKey = getCacheKey(joinPoint, cacheableGourmet);
        log.debug("美食缓存键: {}", cacheKey);

        // 从缓存获取数据
        Object cacheValue = redisUtil.get(cacheKey);
        if (cacheValue != null) {
            log.debug("从缓存读取美食数据: {}", cacheKey);
            try {
                // 使用正确的返回类型进行反序列化
                return JSON.parseObject(cacheValue.toString(), returnType);
            } catch (Exception e) {
                log.error("缓存数据反序列化失败，将执行原方法: {}", e.getMessage());
                // 如果反序列化失败，则执行原方法获取数据
                return joinPoint.proceed();
            }
        }

        // 缓存未命中，执行原方法
        log.debug("缓存未命中，执行原方法获取美食数据: {}", cacheKey);
        Object result = joinPoint.proceed();

        // 写入缓存
        if (result != null && result instanceof Result<?>) {
            String jsonResult = JSON.toJSONString(result);
            redisUtil.set(cacheKey, jsonResult, cacheableGourmet.expire());
            log.debug("缓存美食数据成功: {}", cacheKey);
        }

        return result;
    }

    /**
     * 分类缓存环绕通知
     *
     * @param joinPoint 连接点
     * @return 原方法返回值
     */
    @Around("categoryCachePointcut()")
    public Object categoryCacheAround(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        CacheableCategory cacheableCategory = method.getAnnotation(CacheableCategory.class);

        // 获取返回类型，用于正确反序列化
        Class<?> returnType = method.getReturnType();

        // 获取缓存键
        String cacheKey = CacheConstants.CATEGORY_LIST_KEY;
        log.debug("分类缓存键: {}", cacheKey);

        // 从缓存获取数据
        Object cacheValue = redisUtil.get(cacheKey);
        if (cacheValue != null) {
            log.debug("从缓存读取分类数据: {}", cacheKey);
            try {
                // 使用正确的返回类型进行反序列化
                return JSON.parseObject(cacheValue.toString(), returnType);
            } catch (Exception e) {
                log.error("缓存数据反序列化失败，将执行原方法: {}", e.getMessage());
                // 如果反序列化失败，则执行原方法获取数据
                return joinPoint.proceed();
            }
        }

        // 缓存未命中，执行原方法
        log.debug("缓存未命中，执行原方法获取分类数据");
        Object result = joinPoint.proceed();

        // 写入缓存
        if (result != null && result instanceof Result<?>) {
            String jsonResult = JSON.toJSONString(result);
            redisUtil.set(cacheKey, jsonResult, cacheableCategory.expire());
            log.debug("缓存分类数据成功");
        }

        return result;
    }

    /**
     * 获取缓存键
     *
     * @param joinPoint        连接点
     * @param cacheableGourmet 缓存注解
     * @return 缓存键
     */
    private String getCacheKey(ProceedingJoinPoint joinPoint, CacheableGourmet cacheableGourmet) {
        StringBuilder keyBuilder = new StringBuilder();
        // 根据类型获取基础缓存键
        switch (cacheableGourmet.type()) {
            case SINGLE:
                // 单个美食缓存，获取ID参数
                Object[] args = joinPoint.getArgs();
                if (args != null && args.length > 0) {
                    Object arg = args[0];
                    if (arg instanceof Integer) {
                        return CacheConstants.GOURMET_DETAIL_KEY_PREFIX + arg;
                    } else {
                        // 使用参数的哈希作为键的一部分
                        String paramMd5 = DigestUtils
                                .md5DigestAsHex(JSON.toJSONString(args).getBytes(StandardCharsets.UTF_8));
                        return CacheConstants.GOURMET_DETAIL_KEY_PREFIX + paramMd5;
                    }
                }
                return CacheConstants.GOURMET_DETAIL_KEY_PREFIX + "default";
            case LIST:
                // 美食列表缓存
                Object[] listArgs = joinPoint.getArgs();
                if (listArgs != null && listArgs.length > 0) {
                    // 使用查询参数的哈希作为键的一部分
                    String paramMd5 = DigestUtils
                            .md5DigestAsHex(JSON.toJSONString(listArgs).getBytes(StandardCharsets.UTF_8));
                    return CacheConstants.GOURMET_LIST_KEY + ":" + paramMd5;
                }
                return CacheConstants.GOURMET_LIST_KEY;
            case HOT:
                // 热门美食缓存
                return CacheConstants.GOURMET_HOT_KEY;
            default:
                // 默认使用方法全名作为缓存键
                String className = joinPoint.getTarget().getClass().getSimpleName();
                String methodName = joinPoint.getSignature().getName();
                return "gourmet:cache:" + className + ":" + methodName;
        }
    }
}