package cn.kmbeast.service;

import cn.kmbeast.pojo.entity.Gourmet;
import cn.kmbeast.pojo.vo.ContentRecommendVO;

import java.util.List;

/**
 * 降级服务接口
 * 当Redis不可用时提供备用数据源
 */
public interface FallbackService {

    /**
     * 从数据库获取美食详情（Redis降级方案）
     *
     * @param gourmetId 美食ID
     * @return 美食详情
     */
    Gourmet getGourmetFromDatabase(Integer gourmetId);

    /**
     * 从数据库获取热门美食列表（Redis降级方案）
     *
     * @param limit 数量限制
     * @return 热门美食列表
     */
    List<Gourmet> getTrendingGourmetsFromDatabase(Integer limit);

    /**
     * 从数据库获取推荐内容（Redis降级方案）
     *
     * @param userId 用户ID
     * @param limit 数量限制
     * @return 推荐内容列表
     */
    List<ContentRecommendVO> getRecommendationsFromDatabase(Integer userId, Integer limit);

    /**
     * 检查Redis是否可用
     *
     * @return true-可用，false-不可用
     */
    boolean isRedisAvailable();

    /**
     * 记录Redis异常信息
     *
     * @param operation 操作名称
     * @param exception 异常信息
     */
    void recordRedisException(String operation, Exception exception);

    /**
     * 获取Redis异常统计
     *
     * @return 异常统计信息
     */
    String getRedisExceptionStats();
}
