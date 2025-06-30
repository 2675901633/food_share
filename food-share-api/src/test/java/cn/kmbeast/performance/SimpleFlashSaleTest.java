package cn.kmbeast.performance;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 简化版Redis秒杀并发测试
 * 直接使用Redis操作测试高并发场景
 */
@SpringBootTest
@ActiveProfiles("test")
public class SimpleFlashSaleTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 测试参数
    private static final int THREAD_COUNT = 1000;  // 线程数
    private static final int REQUESTS_PER_THREAD = 10;  // 每个线程的请求次数
    private static final int TOTAL_REQUESTS = THREAD_COUNT * REQUESTS_PER_THREAD;  // 总请求数
    private static final String TEST_ITEM_ID = "9999";  // 测试商品ID
    private static final int INITIAL_STOCK = 100;  // 初始库存

    // 统计变量
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failCount = new AtomicInteger(0);
    private final AtomicInteger duplicateCount = new AtomicInteger(0);
    private final AtomicInteger soldOutCount = new AtomicInteger(0);

    @Test
    public void testRedisFlashSaleConcurrency() throws InterruptedException {
        System.out.println("=== Redis秒杀系统高并发测试开始 ===");
        System.out.println("测试参数: " + THREAD_COUNT + " 个线程，每线程 " + REQUESTS_PER_THREAD + " 次请求，总计 " + TOTAL_REQUESTS + " 次请求");
        System.out.println("初始库存: " + INITIAL_STOCK);

        // 1. 准备测试环境
        setupTestEnvironment();

        // 2. 执行并发测试
        long startTime = System.currentTimeMillis();
        executeConcurrencyTest();
        long endTime = System.currentTimeMillis();

        // 3. 统计结果
        generateTestReport(endTime - startTime);

        // 4. 验证数据一致性
        verifyDataConsistency();
    }

    /**
     * 准备测试环境
     */
    private void setupTestEnvironment() {
        System.out.println("准备测试环境...");
        
        // 设置初始库存
        String stockKey = "flash:stock:" + TEST_ITEM_ID;
        redisTemplate.opsForValue().set(stockKey, INITIAL_STOCK);
        
        // 清理用户记录
        for (int i = 1; i <= THREAD_COUNT; i++) {
            for (int j = 1; j <= REQUESTS_PER_THREAD; j++) {
                int userId = i * 1000 + j;
                String recordKey = "flash:user:record:" + userId + ":" + TEST_ITEM_ID;
                redisTemplate.delete(recordKey);
            }
        }
        
        System.out.println("测试环境准备完成，初始库存: " + redisTemplate.opsForValue().get(stockKey));
    }

    /**
     * 执行并发测试
     */
    private void executeConcurrencyTest() throws InterruptedException {
        System.out.println("开始执行并发测试...");
        
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        // 创建线程池执行秒杀请求
        for (int i = 1; i <= THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    // 每个线程执行多次秒杀请求
                    for (int j = 1; j <= REQUESTS_PER_THREAD; j++) {
                        int userId = threadId * 1000 + j;
                        executeSimpleFlashSale(userId);
                        
                        // 添加小延迟，模拟真实场景
                        Thread.sleep(1);
                    }
                } catch (Exception e) {
                    System.err.println("线程 " + threadId + " 执行异常: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // 等待所有线程完成
        latch.await();
        executor.shutdown();
        
        System.out.println("并发测试执行完成");
    }

    /**
     * 执行简化版秒杀逻辑
     */
    private void executeSimpleFlashSale(int userId) {
        try {
            String stockKey = "flash:stock:" + TEST_ITEM_ID;
            String recordKey = "flash:user:record:" + userId + ":" + TEST_ITEM_ID;
            
            // 1. 检查用户是否已参与
            if (Boolean.TRUE.equals(redisTemplate.hasKey(recordKey))) {
                duplicateCount.incrementAndGet();
                failCount.incrementAndGet();
                return;
            }
            
            // 2. 原子性扣减库存
            Long remainingStock = redisTemplate.opsForValue().decrement(stockKey);
            
            if (remainingStock != null && remainingStock < 0) {
                // 库存不足，恢复库存
                redisTemplate.opsForValue().increment(stockKey);
                soldOutCount.incrementAndGet();
                failCount.incrementAndGet();
                return;
            }
            
            // 3. 记录用户参与
            redisTemplate.opsForValue().set(recordKey, userId, 86400, TimeUnit.SECONDS);
            
            // 4. 秒杀成功
            successCount.incrementAndGet();
            
        } catch (Exception e) {
            failCount.incrementAndGet();
            System.err.println("用户 " + userId + " 秒杀异常: " + e.getMessage());
        }
    }

    /**
     * 生成测试报告
     */
    private void generateTestReport(long duration) {
        System.out.println("=== Redis秒杀系统高并发测试报告 ===");
        System.out.println("测试时长: " + duration + " ms");
        System.out.println("总请求数: " + TOTAL_REQUESTS);
        System.out.println("成功次数: " + successCount.get());
        System.out.println("失败次数: " + failCount.get());
        System.out.printf("成功率: %.2f%%\n", (double) successCount.get() / TOTAL_REQUESTS * 100);
        
        System.out.println("--- 失败原因分析 ---");
        System.out.println("重复参与: " + duplicateCount.get());
        System.out.println("商品售罄: " + soldOutCount.get());
        System.out.println("其他失败: " + (failCount.get() - duplicateCount.get() - soldOutCount.get()));
        
        System.out.println("--- 性能指标 ---");
        System.out.printf("平均QPS: %.2f\n", (double) TOTAL_REQUESTS / duration * 1000);
        System.out.printf("平均响应时间: %.2f ms\n", (double) duration / TOTAL_REQUESTS);
        
        // 获取最终库存
        String stockKey = "flash:stock:" + TEST_ITEM_ID;
        Object finalStock = redisTemplate.opsForValue().get(stockKey);
        int remainingStock = finalStock != null ? Integer.parseInt(finalStock.toString()) : 0;
        int soldStock = INITIAL_STOCK - remainingStock;
        
        System.out.println("--- 库存统计 ---");
        System.out.println("初始库存: " + INITIAL_STOCK);
        System.out.println("剩余库存: " + remainingStock);
        System.out.println("已售库存: " + soldStock);
        System.out.println("库存一致性: " + (soldStock == successCount.get() ? "✅ 一致" : "❌ 不一致"));
    }

    /**
     * 验证数据一致性
     */
    private void verifyDataConsistency() {
        System.out.println("=== 数据一致性验证 ===");
        
        String stockKey = "flash:stock:" + TEST_ITEM_ID;
        Object finalStock = redisTemplate.opsForValue().get(stockKey);
        int remainingStock = finalStock != null ? Integer.parseInt(finalStock.toString()) : 0;
        int soldStock = INITIAL_STOCK - remainingStock;
        
        // 验证库存一致性
        boolean stockConsistent = soldStock == successCount.get();
        System.out.println("库存一致性检查: " + (stockConsistent ? "通过" : "失败"));
        
        if (!stockConsistent) {
            System.err.println("数据不一致！成功订单数: " + successCount.get() + ", 实际销售库存: " + soldStock);
        }
        
        // 验证没有超卖
        boolean noOversell = remainingStock >= 0;
        System.out.println("超卖检查: " + (noOversell ? "通过" : "失败"));
        
        if (!noOversell) {
            System.err.println("发生超卖！剩余库存为负数: " + remainingStock);
        }
        
        // 验证总请求数
        int totalProcessed = successCount.get() + failCount.get();
        boolean requestCountCorrect = totalProcessed == TOTAL_REQUESTS;
        System.out.println("请求数量检查: " + (requestCountCorrect ? "通过" : "失败"));
        
        if (!requestCountCorrect) {
            System.err.println("请求数量不匹配！预期: " + TOTAL_REQUESTS + ", 实际处理: " + totalProcessed);
        }
        
        System.out.println("=== 验证完成 ===");
    }

    /**
     * 高并发压力测试
     */
    @Test
    public void testHighConcurrencyStress() throws InterruptedException {
        System.out.println("=== 高并发压力测试 ===");
        
        final int HIGH_THREAD_COUNT = 2000;
        final int HIGH_REQUESTS_PER_THREAD = 5;
        final int HIGH_INITIAL_STOCK = 50;
        
        // 重置计数器
        successCount.set(0);
        failCount.set(0);
        duplicateCount.set(0);
        soldOutCount.set(0);
        
        // 设置高并发测试环境
        String stockKey = "flash:stock:" + TEST_ITEM_ID;
        redisTemplate.opsForValue().set(stockKey, HIGH_INITIAL_STOCK);
        
        ExecutorService executor = Executors.newFixedThreadPool(HIGH_THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(HIGH_THREAD_COUNT);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 1; i <= HIGH_THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 1; j <= HIGH_REQUESTS_PER_THREAD; j++) {
                        int userId = threadId * 10000 + j;
                        executeSimpleFlashSale(userId);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        long endTime = System.currentTimeMillis();
        
        System.out.println("高并发压力测试完成");
        System.out.println("线程数: " + HIGH_THREAD_COUNT + ", 每线程请求: " + HIGH_REQUESTS_PER_THREAD + 
                          ", 总请求: " + (HIGH_THREAD_COUNT * HIGH_REQUESTS_PER_THREAD));
        System.out.println("测试时长: " + (endTime - startTime) + " ms");
        System.out.println("成功: " + successCount.get() + ", 失败: " + failCount.get());
        System.out.printf("QPS: %.2f\n", (double) (HIGH_THREAD_COUNT * HIGH_REQUESTS_PER_THREAD) / (endTime - startTime) * 1000);
    }
}
