package cn.kmbeast.schedule;

import cn.kmbeast.service.UserBehaviorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 用户行为数据同步定时任务
 * 定期将Redis中的用户行为数据同步到MySQL数据库
 */
@Slf4j
@Component
public class UserBehaviorSyncScheduler {

    @Resource
    private UserBehaviorService userBehaviorService;

    /**
     * 每天凌晨4点同步用户行为数据到数据库
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void syncUserBehaviorToDatabase() {
        log.info("开始执行用户行为数据同步任务...");
        userBehaviorService.syncUserBehaviorToDatabase();
        log.info("用户行为数据同步任务执行完毕");
    }
}