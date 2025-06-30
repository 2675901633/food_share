package cn.kmbeast.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 用户偏好数据VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 分类偏好映射，key为分类ID，value为偏好分数
     */
    private Map<Integer, Double> categoryPreferences;

    /**
     * 总浏览数量
     */
    private Integer totalViewCount;

    /**
     * 最常浏览的分类ID
     */
    private Integer favoriteCategory;

    /**
     * 最常浏览的分类名称
     */
    private String favoriteCategoryName;
}