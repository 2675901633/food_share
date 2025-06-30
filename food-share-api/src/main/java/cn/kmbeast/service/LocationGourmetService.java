package cn.kmbeast.service;

import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.vo.GourmetListVO;

import java.util.List;

/**
 * 基于地理位置的美食推荐服务接口
 */
public interface LocationGourmetService {

    /**
     * 获取附近的美食推荐
     * 
     * @param longitude 经度
     * @param latitude 纬度
     * @param radius 搜索半径（米）
     * @param limit 返回数量限制
     * @return 附近的美食列表
     */
    Result<List<GourmetListVO>> getNearbyGourmets(Double longitude, Double latitude, Integer radius, Integer limit);

    /**
     * 添加美食的地理位置信息
     * 
     * @param gourmetId 美食ID
     * @param longitude 经度
     * @param latitude 纬度
     * @param locationName 位置名称
     * @return 操作结果
     */
    Result<String> addGourmetLocation(Integer gourmetId, Double longitude, Double latitude, String locationName);

    /**
     * 批量初始化美食位置数据（用于演示）
     * 
     * @return 操作结果
     */
    Result<String> initDemoLocations();

    /**
     * 获取同城热门美食（基于当前用户位置）
     * 
     * @param longitude 用户经度
     * @param latitude 用户纬度
     * @param cityRadius 同城范围（米）
     * @param limit 返回数量限制
     * @return 同城热门美食列表
     */
    Result<List<GourmetListVO>> getCityPopularGourmets(Double longitude, Double latitude, Integer cityRadius, Integer limit);
}
