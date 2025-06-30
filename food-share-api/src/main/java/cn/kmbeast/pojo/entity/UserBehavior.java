package cn.kmbeast.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户行为实体类
 */
@Data
@TableName("user_behavior")
public class UserBehavior {
    
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    /**
     * 用户ID
     */
    private Integer userId;
    
    /**
     * 美食ID
     */
    private Integer gourmetId;
    
    /**
     * 行为类型：VIEW（浏览）、LIKE（点赞）、COMMENT（评论）、COLLECT（收藏）
     */
    private String behaviorType;
    
    /**
     * 行为发生时间
     */
    private LocalDateTime createTime;
    
    /**
     * 行为详情（如评论内容）
     */
    private String detail;
}
