package cn.kmbeast.schedule;

import cn.kmbeast.service.RecommendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 热门美食排行榜定时更新任务
 * 定期将数据库中的浏览量数据同步到Redis排行榜
 */
@Slf4j
@Component
public class TrendingFoodsScheduler {

    @Resource
    private RecommendService recommendService;

    /**
     * 每小时更新热门排行榜
     */
    @Scheduled(fixedRate = 3600000) // 1小时
    public void updateTrendingFoods() {
        log.info("开始执行热门排行榜更新任务...");
        recommendService.updateTrendingFoods();
        log.info("热门排行榜更新任务执行完毕");
    }
}