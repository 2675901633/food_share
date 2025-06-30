package cn.kmbeast.service;

import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.vo.NearbyRestaurantVO;

import java.util.List;

/**
 * 地理位置服务接口
 * 基于Redis GEO功能实现附近餐厅推荐
 */
public interface GeoLocationService {

    /**
     * 添加餐厅地理位置信息
     *
     * @param restaurantId 餐厅ID
     * @param longitude 经度
     * @param latitude 纬度
     * @param restaurantName 餐厅名称
     * @return 操作结果
     */
    Result<String> addRestaurantLocation(Integer restaurantId, Double longitude, 
                                       Double latitude, String restaurantName);

    /**
     * 批量添加餐厅位置信息
     *
     * @param restaurants 餐厅位置信息列表
     * @return 操作结果
     */
    Result<String> batchAddRestaurantLocations(List<NearbyRestaurantVO> restaurants);

    /**
     * 查找附近的餐厅
     *
     * @param longitude 用户经度
     * @param latitude 用户纬度
     * @param radius 搜索半径（米）
     * @param limit 返回数量限制
     * @return 附近餐厅列表
     */
    Result<List<NearbyRestaurantVO>> findNearbyRestaurants(Double longitude, Double latitude, 
                                                          Double radius, Integer limit);

    /**
     * 计算两个餐厅之间的距离
     *
     * @param restaurantId1 餐厅1 ID
     * @param restaurantId2 餐厅2 ID
     * @return 距离（米）
     */
    Result<Double> calculateDistance(Integer restaurantId1, Integer restaurantId2);

    /**
     * 获取餐厅的地理位置坐标
     *
     * @param restaurantId 餐厅ID
     * @return 经纬度坐标
     */
    Result<Double[]> getRestaurantPosition(Integer restaurantId);

    /**
     * 删除餐厅位置信息
     *
     * @param restaurantId 餐厅ID
     * @return 操作结果
     */
    Result<String> removeRestaurantLocation(Integer restaurantId);

    /**
     * 获取指定区域内的所有餐厅
     *
     * @param centerLongitude 中心点经度
     * @param centerLatitude 中心点纬度
     * @param width 区域宽度（米）
     * @param height 区域高度（米）
     * @return 区域内餐厅列表
     */
    Result<List<NearbyRestaurantVO>> getRestaurantsInArea(Double centerLongitude, Double centerLatitude,
                                                         Double width, Double height);
}
