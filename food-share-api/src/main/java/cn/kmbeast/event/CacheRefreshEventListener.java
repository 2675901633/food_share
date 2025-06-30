package cn.kmbeast.event;

import cn.kmbeast.utils.CacheConstants;
import cn.kmbeast.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 缓存刷新事件监听器
 * 监听数据变更事件并更新缓存
 */
@Slf4j
@Component
public class CacheRefreshEventListener {

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 监听美食变更事件，更新相关缓存
     * 
     * @param event 美食变更事件
     */
    @EventListener
    public void handleGourmetChangeEvent(GourmetChangeEvent event) {
        if (event == null || event.getGourmetId() == null) {
            return;
        }

        Integer gourmetId = event.getGourmetId();
        log.info("监听到美食变更事件，美食ID: {}, 操作类型: {}", gourmetId, event.getOperationType());

        // 删除单个美食缓存
        String detailKey = CacheConstants.GOURMET_DETAIL_KEY_PREFIX + gourmetId;
        redisUtil.del(detailKey);
        log.info("已删除美食详情缓存: {}", detailKey);

        // 删除美食列表缓存
        redisUtil.delByPrefix(CacheConstants.GOURMET_LIST_KEY);
        log.info("已删除美食列表缓存");

        // 删除热门美食缓存
        redisUtil.del(CacheConstants.GOURMET_HOT_KEY);
        log.info("已删除热门美食缓存");

        // 如果是新增或更新操作，可以预热缓存
        if (event.getOperationType() == OperationType.CREATE || event.getOperationType() == OperationType.UPDATE) {
            // 预热缓存逻辑可以在这里实现
            // 例如调用相关服务获取数据并缓存
        }
    }

    /**
     * 监听分类变更事件，更新相关缓存
     * 
     * @param event 分类变更事件
     */
    @EventListener
    public void handleCategoryChangeEvent(CategoryChangeEvent event) {
        if (event == null) {
            return;
        }

        log.info("监听到分类变更事件");

        // 删除分类列表缓存
        redisUtil.del(CacheConstants.CATEGORY_LIST_KEY);
        log.info("已删除分类列表缓存");

        // 删除美食列表缓存（因为可能按分类查询）
        redisUtil.delByPrefix(CacheConstants.GOURMET_LIST_KEY);
        log.info("已删除美食列表缓存");
    }
}