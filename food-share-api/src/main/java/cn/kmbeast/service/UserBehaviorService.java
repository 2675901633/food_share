package cn.kmbeast.service;

import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.vo.GourmetVO;
import cn.kmbeast.pojo.vo.UserPreferenceVO;

import java.util.List;
import java.util.Map;

/**
 * 用户行为服务接口
 * 处理用户行为数据的缓存和分析
 */
public interface UserBehaviorService {

    /**
     * 记录用户浏览美食的行为
     * 
     * @param userId     用户ID
     * @param gourmetId  美食ID
     * @param categoryId 分类ID
     * @return 操作结果
     */
    Result<Void> recordUserView(Integer userId, Integer gourmetId, Integer categoryId);

    /**
     * 获取用户浏览历史
     * 
     * @param userId 用户ID
     * @param limit  返回记录数量限制
     * @return 用户浏览历史列表
     */
    Result<List<GourmetVO>> getUserViewHistory(Integer userId, Integer limit);

    /**
     * 获取用户最近浏览的美食
     * 
     * @param userId 用户ID
     * @param limit  返回记录数量限制
     * @return 最近浏览的美食列表
     */
    Result<List<GourmetVO>> getUserRecentViews(Integer userId, Integer limit);

    /**
     * 获取用户偏好数据
     * 
     * @param userId 用户ID
     * @return 用户偏好数据
     */
    Result<UserPreferenceVO> getUserPreferences(Integer userId);

    /**
     * 更新用户分类偏好
     * 
     * @param userId     用户ID
     * @param categoryId 分类ID
     * @return 操作结果
     */
    Result<Void> updateUserCategoryPreference(Integer userId, Integer categoryId);

    /**
     * 清除用户浏览历史
     * 
     * @param userId 用户ID
     * @return 操作结果
     */
    Result<Void> clearUserViewHistory(Integer userId);

    /**
     * 同步用户行为数据到数据库
     * 定期将Redis中的用户行为数据持久化到MySQL
     * 
     * @return 操作结果
     */
    Result<Void> syncUserBehaviorToDatabase();
}