package cn.kmbeast.controller;

import cn.kmbeast.context.LocalThreadHolder;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.dto.query.extend.ContentBasedQueryDto;
import cn.kmbeast.pojo.vo.ContentRecommendVO;
import cn.kmbeast.pojo.vo.GourmetVO;
import cn.kmbeast.service.ContentRecommendService;
import cn.kmbeast.service.RecommendService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * 推荐控制器
 */
@RestController
@RequestMapping("/recommend")
public class RecommendController {

    @Resource
    private RecommendService recommendService;

    @Resource
    private ContentRecommendService contentRecommendService;

    /**
     * 查询推荐的美食做法数据
     *
     * @param item 需要的推荐的条数
     * @return Result<List<GourmetVO>> 响应结果
     */
    @GetMapping(value = "/{item}")
    public Result<List<GourmetVO>> recommendGourmet(@PathVariable Integer item) {
        return recommendService.recommendGourmet(item);
    }

    /**
     * 获取相似美食推荐
     *
     * @param gourmetId 美食ID
     * @param size      推荐数量
     * @return Result<List<ContentRecommendVO>> 响应结果
     */
    @GetMapping("/similar")
    public Result<List<ContentRecommendVO>> getSimilarRecommends(
            @RequestParam Integer gourmetId,
            @RequestParam(defaultValue = "5") Integer size) {
        ContentBasedQueryDto queryDto = new ContentBasedQueryDto();
        queryDto.setGourmetId(gourmetId);
        queryDto.setSize(size);
        return contentRecommendService.recommendSimilar(queryDto);
    }

    /**
     * 获取热门美食排行榜
     *
     * @param limit 返回数量
     * @return Result<List<GourmetVO>> 响应结果
     */
    @GetMapping("/trending")
    public Result<List<GourmetVO>> getTrendingFoods(
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        return recommendService.getTrendingFoods(limit);
    }

    /**
     * 记录美食UV
     *
     * @param gourmetId 美食ID
     * @return Result<Void> 响应结果
     */
    @PostMapping("/uv/{gourmetId}")
    public Result<Void> recordGourmetUV(@PathVariable Integer gourmetId) {
        Integer userId = LocalThreadHolder.getUserId();
        return recommendService.recordGourmetUV(gourmetId, userId);
    }

    /**
     * 获取美食UV统计
     *
     * @param gourmetId 美食ID
     * @return Result<Long> 响应结果
     */
    @GetMapping("/uv/{gourmetId}")
    public Result<Long> getGourmetUV(@PathVariable Integer gourmetId) {
        return recommendService.getGourmetUV(gourmetId);
    }

    /**
     * 更新热门排行榜
     *
     * @return Result<Void> 响应结果
     */
    @PostMapping("/trending/update")
    public Result<Void> updateTrendingFoods() {
        return recommendService.updateTrendingFoods();
    }

    /**
     * 获取推荐系统效果评估指标
     *
     * @return Result<Map<String, Object>> 响应结果
     */
    @GetMapping("/metrics/evaluate")
    public Result<Map<String, Object>> evaluateRecommendationMetrics() {
        // 创建模拟的推荐效果评估数据
        Map<String, Object> metrics = new HashMap<>();

        // 当前用户的推荐效果
        Map<String, Double> currentUserMetrics = new HashMap<>();
        currentUserMetrics.put("precision", 0.85);
        currentUserMetrics.put("recall", 0.72);
        metrics.put("current_user", currentUserMetrics);

        // 全局推荐效果
        Map<String, Double> globalMetrics = new HashMap<>();
        globalMetrics.put("precision", 0.78);
        globalMetrics.put("adjusted_recall", 0.65);
        metrics.put("global_metrics", globalMetrics);

        return ApiResult.success(metrics);
    }
}
