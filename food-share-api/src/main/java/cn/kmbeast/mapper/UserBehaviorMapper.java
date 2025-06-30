package cn.kmbeast.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户行为数据访问层
 */
@Mapper
public interface UserBehaviorMapper {
    
    /**
     * 获取与指定美食有交互的用户ID列表
     */
    @Select("SELECT DISTINCT user_id FROM user_behavior WHERE gourmet_id = #{gourmetId}")
    List<Integer> getUsersByGourmet(@Param("gourmetId") Integer gourmetId);
    
    /**
     * 获取用户对美食的浏览次数
     */
    @Select("SELECT COUNT(*) FROM user_behavior WHERE user_id = #{userId} AND gourmet_id = #{gourmetId} AND behavior_type = 'VIEW'")
    int getViewCount(@Param("userId") Integer userId, @Param("gourmetId") Integer gourmetId);
    
    /**
     * 获取用户对美食的点赞次数
     */
    @Select("SELECT COUNT(*) FROM user_behavior WHERE user_id = #{userId} AND gourmet_id = #{gourmetId} AND behavior_type = 'LIKE'")
    int getLikeCount(@Param("userId") Integer userId, @Param("gourmetId") Integer gourmetId);
    
    /**
     * 获取用户对美食的评论次数
     */
    @Select("SELECT COUNT(*) FROM user_behavior WHERE user_id = #{userId} AND gourmet_id = #{gourmetId} AND behavior_type = 'COMMENT'")
    int getCommentCount(@Param("userId") Integer userId, @Param("gourmetId") Integer gourmetId);
    
    /**
     * 获取用户对美食的收藏次数
     */
    @Select("SELECT COUNT(*) FROM user_behavior WHERE user_id = #{userId} AND gourmet_id = #{gourmetId} AND behavior_type = 'COLLECT'")
    int getCollectCount(@Param("userId") Integer userId, @Param("gourmetId") Integer gourmetId);
}
