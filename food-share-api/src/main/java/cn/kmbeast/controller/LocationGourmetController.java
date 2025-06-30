package cn.kmbeast.controller;

import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.vo.GourmetListVO;
import cn.kmbeast.service.LocationGourmetService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 基于地理位置的美食推荐控制器
 */
@RestController
@CrossOrigin
@RequestMapping("/location-gourmet")
public class LocationGourmetController {

    @Resource
    private LocationGourmetService locationGourmetService;

    /**
     * 获取附近的美食推荐
     * 
     * @param longitude 经度
     * @param latitude  纬度
     * @param radius    搜索半径（米）
     * @param limit     返回数量限制
     * @return 附近的美食列表
     */
    @GetMapping("/nearby")
    @ResponseBody
    public Result<List<GourmetListVO>> getNearbyGourmets(
            @RequestParam Double longitude,
            @RequestParam Double latitude,
            @RequestParam(defaultValue = "5000") Integer radius,
            @RequestParam(defaultValue = "20") Integer limit) {
        return locationGourmetService.getNearbyGourmets(longitude, latitude, radius, limit);
    }

    /**
     * 添加美食的地理位置信息
     *
     * @param request 位置信息请求
     * @return 操作结果
     */
    @PostMapping("/add-location")
    @ResponseBody
    public Result<String> addGourmetLocation(@RequestBody AddLocationRequest request) {
        return locationGourmetService.addGourmetLocation(
                request.getGourmetId(),
                request.getLongitude(),
                request.getLatitude(),
                request.getLocationName());
    }

    /**
     * 位置信息请求DTO
     */
    public static class AddLocationRequest {
        private Integer gourmetId;
        private Double longitude;
        private Double latitude;
        private String locationName;

        // Getters and Setters
        public Integer getGourmetId() {
            return gourmetId;
        }

        public void setGourmetId(Integer gourmetId) {
            this.gourmetId = gourmetId;
        }

        public Double getLongitude() {
            return longitude;
        }

        public void setLongitude(Double longitude) {
            this.longitude = longitude;
        }

        public Double getLatitude() {
            return latitude;
        }

        public void setLatitude(Double latitude) {
            this.latitude = latitude;
        }

        public String getLocationName() {
            return locationName;
        }

        public void setLocationName(String locationName) {
            this.locationName = locationName;
        }
    }

    /**
     * 批量初始化美食位置数据（用于演示）
     * 
     * @return 操作结果
     */
    @PostMapping("/init-demo-locations")
    @ResponseBody
    public Result<String> initDemoLocations() {
        return locationGourmetService.initDemoLocations();
    }

    /**
     * 获取同城热门美食（基于当前用户位置）
     * 
     * @param longitude  用户经度
     * @param latitude   用户纬度
     * @param cityRadius 同城范围（米，默认50公里）
     * @param limit      返回数量限制
     * @return 同城热门美食列表
     */
    @GetMapping("/city-popular")
    @ResponseBody
    public Result<List<GourmetListVO>> getCityPopularGourmets(
            @RequestParam Double longitude,
            @RequestParam Double latitude,
            @RequestParam(defaultValue = "50000") Integer cityRadius,
            @RequestParam(defaultValue = "10") Integer limit) {
        return locationGourmetService.getCityPopularGourmets(longitude, latitude, cityRadius, limit);
    }
}
