package cn.kmbeast.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 美食做法列表VO类
 */
@Data
@NoArgsConstructor
public class GourmetListVO {

    /**
     * 主键ID
     */
    private Integer id;
    /**
     * 标题
     */
    private String title;
    /**
     * 封面
     */
    private String cover;
    /**
     * 简要
     */
    private String detail;
    /**
     * 用户名
     */
    private String userName;
    /**
     * 用户的头像
     */
    private String userAvatar;
    /**
     * 浏览量
     */
    private Integer viewCount;
    /**
     * 点赞量
     */
    private Integer upvoteCount;
    /**
     * 收藏量
     */
    private Integer saveCount;
    /**
     * 评分
     */
    private Double rating;
    /**
     * 发布时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 发布位置名称
     */
    private String locationName;

    /**
     * 距离当前用户的距离（米）- 仅在地理位置查询时使用
     */
    private Double distance;

    // 原有的构造函数（兼容现有代码）
    public GourmetListVO(Integer id, String title, String cover, String detail, String userName,
            String userAvatar, Integer viewCount, Integer upvoteCount, Integer saveCount,
            Double rating, LocalDateTime createTime) {
        this.id = id;
        this.title = title;
        this.cover = cover;
        this.detail = detail;
        this.userName = userName;
        this.userAvatar = userAvatar;
        this.viewCount = viewCount;
        this.upvoteCount = upvoteCount;
        this.saveCount = saveCount;
        this.rating = rating;
        this.createTime = createTime;
        this.locationName = null;
        this.distance = null;
    }

    // 包含位置信息的构造函数
    public GourmetListVO(Integer id, String title, String cover, String detail, String userName,
            String userAvatar, Integer viewCount, Integer upvoteCount, Integer saveCount,
            Double rating, LocalDateTime createTime, String locationName, Double distance) {
        this.id = id;
        this.title = title;
        this.cover = cover;
        this.detail = detail;
        this.userName = userName;
        this.userAvatar = userAvatar;
        this.viewCount = viewCount;
        this.upvoteCount = upvoteCount;
        this.saveCount = saveCount;
        this.rating = rating;
        this.createTime = createTime;
        this.locationName = locationName;
        this.distance = distance;
    }
}
