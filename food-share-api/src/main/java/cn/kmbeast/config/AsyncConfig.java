package cn.kmbeast.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步处理配置
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${app.performance.thread-pool-size:50}")
    private int threadPoolSize;

    @Value("${app.performance.queue-capacity:1000}")
    private int queueCapacity;

    /**
     * 异步任务线程池
     */
    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数
        executor.setCorePoolSize(threadPoolSize / 2);
        // 最大线程数
        executor.setMaxPoolSize(threadPoolSize);
        // 队列容量
        executor.setQueueCapacity(queueCapacity);
        // 线程名前缀
        executor.setThreadNamePrefix("food-share-async-");
        // 拒绝策略：由调用线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 等待时间
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        log.info("异步任务线程池初始化完成 - 核心线程数: {}, 最大线程数: {}, 队列容量: {}",
                threadPoolSize / 2, threadPoolSize, queueCapacity);

        return executor;
    }

    /**
     * 缓存预热专用线程池
     */
    @Bean("cacheWarmupExecutor")
    public Executor cacheWarmupExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("cache-warmup-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();

        log.info("缓存预热线程池初始化完成");

        return executor;
    }
} 