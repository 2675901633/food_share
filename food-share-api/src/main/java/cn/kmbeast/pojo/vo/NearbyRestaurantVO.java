package cn.kmbeast.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.geo.Metric;

/**
 * 附近餐厅信息VO
 * 用于地理位置搜索结果展示
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NearbyRestaurantVO {

    /**
     * 餐厅ID
     */
    private Integer restaurantId;

    /**
     * 餐厅名称
     */
    private String restaurantName;

    /**
     * 餐厅地址
     */
    private String address;

    /**
     * 经度
     */
    private Double longitude;

    /**
     * 纬度
     */
    private Double latitude;

    /**
     * 距离用户的距离
     */
    private Double distance;

    /**
     * 距离单位
     */
    private Metric distanceUnit;

    /**
     * 餐厅评分
     */
    private Double rating;

    /**
     * 餐厅类型
     */
    private String cuisineType;

    /**
     * 营业状态（0-休息中，1-营业中）
     */
    private Integer businessStatus;

    /**
     * 餐厅图片URL
     */
    private String imageUrl;

    /**
     * 人均消费
     */
    private Integer avgPrice;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 营业时间
     */
    private String businessHours;

    /**
     * 构造函数 - 基本信息
     */
    public NearbyRestaurantVO(Integer restaurantId, String restaurantName,
            Double longitude, Double latitude, String address) {
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
        this.longitude = longitude;
        this.latitude = latitude;
        this.address = address;
    }
}
