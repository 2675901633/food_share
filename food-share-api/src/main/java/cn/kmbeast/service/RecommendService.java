package cn.kmbeast.service;

import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.vo.GourmetVO;

import java.util.List;
import java.util.Map;

public interface RecommendService {

    /**
     * 根据用户历史行为推荐美食
     * 
     * @param item 推荐数量
     * @return 推荐的美食列表
     */
    Result<List<GourmetVO>> recommendGourmet(Integer item);

    /**
     * 获取热门美食排行榜
     * 
     * @param limit 返回数量
     * @return 热门美食列表
     */
    Result<List<GourmetVO>> getTrendingFoods(Integer limit);

    /**
     * 记录美食UV
     * 
     * @param gourmetId 美食ID
     * @param userId    用户ID
     * @return 操作结果
     */
    Result<Void> recordGourmetUV(Integer gourmetId, Integer userId);

    /**
     * 获取美食UV统计
     * 
     * @param gourmetId 美食ID
     * @return UV数量
     */
    Result<Long> getGourmetUV(Integer gourmetId);

    /**
     * 更新热门排行榜
     * 同步数据库中的浏览量数据到Redis排行榜
     * 
     * @return 操作结果
     */
    Result<Void> updateTrendingFoods();
}
