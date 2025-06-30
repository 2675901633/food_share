package cn.kmbeast.performance;

import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.entity.FlashSaleOrder;
import cn.kmbeast.service.FlashSaleService;
import cn.kmbeast.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.HashMap;

/**
 * 秒杀系统高并发测试
 * 测试1000个线程循环10次请求的情况下，能成功秒杀多少库存
 */
@Slf4j
@SpringBootTest
public class FlashSaleConcurrencyTest {

    @Autowired
    private FlashSaleService flashSaleService;

    @Autowired
    private RedisUtil redisUtil;

    // 测试参数
    private static final int THREAD_COUNT = 1000; // 线程数
    private static final int REQUESTS_PER_THREAD = 10; // 每个线程的请求次数
    private static final int TOTAL_REQUESTS = THREAD_COUNT * REQUESTS_PER_THREAD; // 总请求数
    private static final Integer TEST_ITEM_ID = 9999; // 测试商品ID
    private static final int INITIAL_STOCK = 100; // 初始库存

    // 统计变量
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failCount = new AtomicInteger(0);
    private final AtomicInteger duplicateCount = new AtomicInteger(0);
    private final AtomicInteger systemBusyCount = new AtomicInteger(0);
    private final AtomicInteger soldOutCount = new AtomicInteger(0);
    private final AtomicInteger rateLimitCount = new AtomicInteger(0);

    @Test
    public void testFlashSaleConcurrency() throws InterruptedException {
        log.info("=== 秒杀系统高并发测试开始 ===");
        log.info("测试参数: {} 个线程，每线程 {} 次请求，总计 {} 次请求",
                THREAD_COUNT, REQUESTS_PER_THREAD, TOTAL_REQUESTS);
        log.info("初始库存: {}", INITIAL_STOCK);

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
        log.info("准备测试环境...");

        try {
            // 1. 创建测试商品数据
            createTestFlashSaleItem();

            // 2. 清理Redis中的相关数据
            String stockKey = "flash:stock:" + TEST_ITEM_ID;
            String lockKey = "flash:lock:" + TEST_ITEM_ID;

            // 3. 设置初始库存
            redisUtil.set(stockKey, INITIAL_STOCK, 3600);

            // 4. 清理可能存在的锁
            redisUtil.del(lockKey);

            // 5. 清理用户记录和限流记录
            for (int i = 1; i <= Math.min(THREAD_COUNT, 100); i++) { // 限制清理数量，避免过多操作
                for (int j = 1; j <= Math.min(REQUESTS_PER_THREAD, 10); j++) {
                    int userId = i * 1000 + j;
                    String recordKey = "flash:user:record:" + userId + ":" + TEST_ITEM_ID;
                    String rateLimitKey = "flash:rate:limit:" + userId + ":" + TEST_ITEM_ID;
                    redisUtil.del(recordKey);
                    redisUtil.del(rateLimitKey);
                }
            }

            log.info("测试环境准备完成，初始库存: {}", redisUtil.get(stockKey));
        } catch (Exception e) {
            log.error("准备测试环境失败: {}", e.getMessage(), e);
            throw new RuntimeException("测试环境准备失败", e);
        }
    }

    /**
     * 创建测试用的秒杀商品
     */
    private void createTestFlashSaleItem() {
        try {
            // 检查商品是否已存在
            String itemKey = "flash:item:" + TEST_ITEM_ID;
            Object existingItem = redisUtil.get(itemKey);

            if (existingItem == null) {
                // 创建测试商品缓存数据
                Map<String, Object> testItem = new HashMap<>();
                testItem.put("id", TEST_ITEM_ID);
                testItem.put("name", "测试秒杀商品");
                testItem.put("price", 99.99);
                testItem.put("stock", INITIAL_STOCK);
                testItem.put("status", 1); // 1表示进行中
                testItem.put("startTime", System.currentTimeMillis() - 3600000); // 1小时前开始
                testItem.put("endTime", System.currentTimeMillis() + 3600000); // 1小时后结束

                // 缓存商品信息
                redisUtil.set(itemKey, testItem, 3600);
                log.info("创建测试商品缓存: {}", testItem);
            }
        } catch (Exception e) {
            log.warn("创建测试商品失败，将使用简化测试: {}", e.getMessage());
        }
    }

    /**
     * 执行并发测试
     */
    private void executeConcurrencyTest() throws InterruptedException {
        log.info("开始执行并发测试...");

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
                        executeFlashSale(userId);

                        // 添加小延迟，模拟真实场景
                        Thread.sleep(1);
                    }
                } catch (Exception e) {
                    log.error("线程 {} 执行异常: {}", threadId, e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // 等待所有线程完成
        latch.await();
        executor.shutdown();

        log.info("并发测试执行完成");
    }

    /**
     * 执行单次秒杀
     */
    private void executeFlashSale(int userId) {
        try {
            Result<FlashSaleOrder> result = flashSaleService.flashSale(TEST_ITEM_ID, userId);

            if (result.isSuccess()) {
                successCount.incrementAndGet();
                log.debug("用户 {} 秒杀成功", userId);
            } else {
                failCount.incrementAndGet();
                String message = result.getMsg();

                // 统计不同类型的失败
                if (message.contains("已参与过")) {
                    duplicateCount.incrementAndGet();
                } else if (message.contains("系统繁忙")) {
                    systemBusyCount.incrementAndGet();
                } else if (message.contains("已售罄")) {
                    soldOutCount.incrementAndGet();
                } else if (message.contains("请求过于频繁")) {
                    rateLimitCount.incrementAndGet();
                }

                log.debug("用户 {} 秒杀失败: {}", userId, message);
            }
        } catch (Exception e) {
            failCount.incrementAndGet();
            log.error("用户 {} 秒杀异常: {}", userId, e.getMessage());
        }
    }

    /**
     * 生成测试报告
     */
    private void generateTestReport(long duration) {
        log.info("=== 秒杀系统高并发测试报告 ===");
        log.info("测试时长: {} ms", duration);
        log.info("总请求数: {}", TOTAL_REQUESTS);
        log.info("成功次数: {}", successCount.get());
        log.info("失败次数: {}", failCount.get());
        log.info("成功率: {:.2f}%", (double) successCount.get() / TOTAL_REQUESTS * 100);

        log.info("--- 失败原因分析 ---");
        log.info("重复参与: {}", duplicateCount.get());
        log.info("系统繁忙: {}", systemBusyCount.get());
        log.info("商品售罄: {}", soldOutCount.get());
        log.info("请求限流: {}", rateLimitCount.get());
        log.info("其他失败: {}", failCount.get() - duplicateCount.get() - systemBusyCount.get()
                - soldOutCount.get() - rateLimitCount.get());

        log.info("--- 性能指标 ---");
        log.info("平均QPS: {:.2f}", (double) TOTAL_REQUESTS / duration * 1000);
        log.info("平均响应时间: {:.2f} ms", (double) duration / TOTAL_REQUESTS);

        // 获取最终库存
        String stockKey = "flash:stock:" + TEST_ITEM_ID;
        Object finalStock = redisUtil.get(stockKey);
        int remainingStock = finalStock != null ? Integer.parseInt(finalStock.toString()) : 0;
        int soldStock = INITIAL_STOCK - remainingStock;

        log.info("--- 库存统计 ---");
        log.info("初始库存: {}", INITIAL_STOCK);
        log.info("剩余库存: {}", remainingStock);
        log.info("已售库存: {}", soldStock);
        log.info("库存一致性: {}", soldStock == successCount.get() ? "✅ 一致" : "❌ 不一致");
    }

    /**
     * 验证数据一致性
     */
    private void verifyDataConsistency() {
        log.info("=== 数据一致性验证 ===");

        String stockKey = "flash:stock:" + TEST_ITEM_ID;
        Object finalStock = redisUtil.get(stockKey);
        int remainingStock = finalStock != null ? Integer.parseInt(finalStock.toString()) : 0;
        int soldStock = INITIAL_STOCK - remainingStock;

        // 验证库存一致性
        boolean stockConsistent = soldStock == successCount.get();
        log.info("库存一致性检查: {}", stockConsistent ? "通过" : "失败");

        if (!stockConsistent) {
            log.error("数据不一致！成功订单数: {}, 实际销售库存: {}", successCount.get(), soldStock);
        }

        // 验证没有超卖
        boolean noOversell = remainingStock >= 0;
        log.info("超卖检查: {}", noOversell ? "通过" : "失败");

        if (!noOversell) {
            log.error("发生超卖！剩余库存为负数: {}", remainingStock);
        }

        // 验证总请求数
        int totalProcessed = successCount.get() + failCount.get();
        boolean requestCountCorrect = totalProcessed == TOTAL_REQUESTS;
        log.info("请求数量检查: {}", requestCountCorrect ? "通过" : "失败");

        if (!requestCountCorrect) {
            log.error("请求数量不匹配！预期: {}, 实际处理: {}", TOTAL_REQUESTS, totalProcessed);
        }

        log.info("=== 验证完成 ===");
    }

    /**
     * 压力测试 - 更高并发
     */
    @Test
    public void testHighConcurrencyStress() throws InterruptedException {
        log.info("=== 高并发压力测试 ===");

        final int HIGH_THREAD_COUNT = 2000;
        final int HIGH_REQUESTS_PER_THREAD = 5;
        final int HIGH_INITIAL_STOCK = 50;

        // 重置计数器
        successCount.set(0);
        failCount.set(0);
        duplicateCount.set(0);
        systemBusyCount.set(0);
        soldOutCount.set(0);
        rateLimitCount.set(0);

        // 设置高并发测试环境
        String stockKey = "flash:stock:" + TEST_ITEM_ID;
        redisUtil.set(stockKey, HIGH_INITIAL_STOCK, 3600);

        ExecutorService executor = Executors.newFixedThreadPool(HIGH_THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(HIGH_THREAD_COUNT);

        long startTime = System.currentTimeMillis();

        for (int i = 1; i <= HIGH_THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 1; j <= HIGH_REQUESTS_PER_THREAD; j++) {
                        int userId = threadId * 10000 + j;
                        executeFlashSale(userId);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long endTime = System.currentTimeMillis();

        log.info("高并发压力测试完成");
        log.info("线程数: {}, 每线程请求: {}, 总请求: {}",
                HIGH_THREAD_COUNT, HIGH_REQUESTS_PER_THREAD, HIGH_THREAD_COUNT * HIGH_REQUESTS_PER_THREAD);
        log.info("测试时长: {} ms", endTime - startTime);
        log.info("成功: {}, 失败: {}", successCount.get(), failCount.get());
        log.info("QPS: {:.2f}", (double) (HIGH_THREAD_COUNT * HIGH_REQUESTS_PER_THREAD) / (endTime - startTime) * 1000);
    }
}
