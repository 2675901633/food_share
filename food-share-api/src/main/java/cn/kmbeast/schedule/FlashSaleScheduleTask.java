package cn.kmbeast.schedule;

import cn.kmbeast.service.FlashSaleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 秒杀系统定时任务
 */
@Slf4j
@Component
@EnableScheduling
public class FlashSaleScheduleTask {

    @Resource
    private FlashSaleService flashSaleService;

    /**
     * 每5分钟刷新秒杀商品状态
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void refreshFlashSaleStatus() {
        try {
            log.info("开始执行秒杀商品状态刷新任务");
            flashSaleService.refreshFlashSaleStatus();
            log.info("秒杀商品状态刷新任务执行完成");
        } catch (Exception e) {
            log.error("秒杀商品状态刷新任务执行失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 每天凌晨1点预热秒杀库存
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void preloadFlashSaleStock() {
        try {
            log.info("开始执行秒杀库存预热任务");
            flashSaleService.preloadFlashSaleStock();
            log.info("秒杀库存预热任务执行完成");
        } catch (Exception e) {
            log.error("秒杀库存预热任务执行失败: {}", e.getMessage(), e);
        }
    }
}