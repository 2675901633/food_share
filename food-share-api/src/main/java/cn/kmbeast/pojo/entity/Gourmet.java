package cn.kmbeast.pojo.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 美食做法表实体类
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Gourmet {

    /**
     * 主键ID
     */
    private Integer id;

    /**
     * 美食分类ID
     */
    private Integer categoryId;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 封面
     */
    private String cover;

    /**
     * 是否已经审核
     */
    private Boolean isAudit;

    /**
     * 是否公开
     */
    private Boolean isPublish;

    /**
     * 发布时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 发布位置 - 经度
     */
    private Double longitude;

    /**
     * 发布位置 - 纬度
     */
    private Double latitude;

    /**
     * 发布位置 - 地址描述
     */
    private String locationName;

}
