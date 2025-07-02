package cn.kmbeast.aop;

import cn.kmbeast.annotation.RateLimit;
import cn.kmbeast.annotation.RateLimitType;
import cn.kmbeast.exception.BusinessException;
import cn.kmbeast.service.impl.RateLimitService;
import cn.kmbeast.utils.LocalThreadHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 限流切面
 */
@Slf4j
@Aspect
@Component
@Order(1) // 确保在其他切面之前执行
public class RateLimitAspect {

    @Autowired
    private RateLimitService rateLimitService;

    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        String key = generateKey(point, rateLimit);

        boolean allowed = rateLimitService.isAllowed(key, rateLimit.limit(), rateLimit.window());

        if (!allowed) {
            log.warn("限流触发 - key: {}, limit: {}, window: {}s", key, rateLimit.limit(), rateLimit.window());
            throw new BusinessException(rateLimit.message());
        }

        return point.proceed();
    }

    /**
     * 生成限流键
     */
    private String generateKey(ProceedingJoinPoint point, RateLimit rateLimit) {
        StringBuilder keyBuilder = new StringBuilder("rate_limit:");

        // 添加方法标识
        keyBuilder.append(point.getSignature().getDeclaringTypeName())
                  .append(":")
                  .append(point.getSignature().getName());

        // 根据限流类型添加标识
        switch (rateLimit.type()) {
            case USER:
                Integer userId = LocalThreadHolder.getUserId();
                keyBuilder.append(":user:").append(userId != null ? userId : "anonymous");
                break;
            case IP:
                String ip = getClientIP();
                keyBuilder.append(":ip:").append(ip);
                break;
            case GLOBAL:
                keyBuilder.append(":global");
                break;
            case CUSTOM:
                if (StringUtils.hasText(rateLimit.key())) {
                    String customKey = parseSpEL(rateLimit.key(), point);
                    keyBuilder.append(":custom:").append(customKey);
                }
                break;
        }

        return keyBuilder.toString();
    }

    /**
     * 解析SpEL表达式
     */
    private String parseSpEL(String spel, ProceedingJoinPoint point) {
        try {
            Expression expression = parser.parseExpression(spel);
            EvaluationContext context = new StandardEvaluationContext();

            // 设置方法参数
            Object[] args = point.getArgs();
            for (int i = 0; i < args.length; i++) {
                context.setVariable("arg" + i, args[i]);
            }

            Object value = expression.getValue(context);
            return value != null ? value.toString() : "";
        } catch (Exception e) {
            log.error("SpEL表达式解析失败: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 获取客户端IP
     */
    private String getClientIP() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("X-Real-IP");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getRemoteAddr();
                }
                return ip;
            }
        } catch (Exception e) {
            log.error("获取客户端IP失败: {}", e.getMessage());
        }
        return "unknown";
    }
} 