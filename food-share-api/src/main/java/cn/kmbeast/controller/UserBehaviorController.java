package cn.kmbeast.controller;

import cn.kmbeast.context.LocalThreadHolder;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.vo.GourmetVO;
import cn.kmbeast.pojo.vo.UserPreferenceVO;
import cn.kmbeast.service.UserBehaviorService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 用户行为控制器
 */
@RestController
@RequestMapping("/behavior")
public class UserBehaviorController {

    @Resource
    private UserBehaviorService userBehaviorService;

    /**
     * 记录用户浏览行为
     * 
     * @param gourmetId  美食ID
     * @param categoryId 分类ID
     * @return 操作结果
     */
    @PostMapping("/view")
    public Result<Void> recordUserView(
            @RequestParam Integer gourmetId,
            @RequestParam(required = false) Integer categoryId) {
        Integer userId = LocalThreadHolder.getUserId();
        return userBehaviorService.recordUserView(userId, gourmetId, categoryId);
    }

    /**
     * 获取用户浏览历史
     * 
     * @param limit 返回记录数量
     * @return 用户浏览历史列表
     */
    @GetMapping("/history")
    public Result<List<GourmetVO>> getUserViewHistory(
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        Integer userId = LocalThreadHolder.getUserId();
        return userBehaviorService.getUserViewHistory(userId, limit);
    }

    /**
     * 获取用户最近浏览
     * 
     * @param limit 返回记录数量
     * @return 用户最近浏览列表
     */
    @GetMapping("/recent")
    public Result<List<GourmetVO>> getUserRecentViews(
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        Integer userId = LocalThreadHolder.getUserId();
        return userBehaviorService.getUserRecentViews(userId, limit);
    }

    /**
     * 获取用户偏好数据
     * 
     * @return 用户偏好数据
     */
    @GetMapping("/preferences")
    public Result<UserPreferenceVO> getUserPreferences() {
        Integer userId = LocalThreadHolder.getUserId();
        return userBehaviorService.getUserPreferences(userId);
    }

    /**
     * 清除用户浏览历史
     * 
     * @return 操作结果
     */
    @DeleteMapping("/history")
    public Result<Void> clearUserViewHistory() {
        Integer userId = LocalThreadHolder.getUserId();
        return userBehaviorService.clearUserViewHistory(userId);
    }
}