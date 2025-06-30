package cn.kmbeast.service.impl;

import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.vo.NearbyRestaurantVO;
import cn.kmbeast.service.GeoLocationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 地理位置服务实现类
 * 基于Redis GEO功能实现附近餐厅推荐
 */
@Slf4j
@Service
public class GeoLocationServiceImpl implements GeoLocationService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String RESTAURANTS_GEO_KEY = "restaurants:geo";

    @Override
    public Result<String> addRestaurantLocation(Integer restaurantId, Double longitude,
            Double latitude, String restaurantName) {
        try {
            log.info("添加餐厅位置，ID: {}, 名称: {}, 经度: {}, 纬度: {}",
                    restaurantId, restaurantName, longitude, latitude);

            // 创建地理位置点
            Point point = new Point(longitude, latitude);
            String member = "restaurant_" + restaurantId;

            // 添加到Redis GEO
            Long result = redisTemplate.opsForGeo().add(RESTAURANTS_GEO_KEY, point, member);

            if (result != null && result > 0) {
                // 同时存储餐厅详细信息
                String infoKey = "restaurant:info:" + restaurantId;
                NearbyRestaurantVO restaurant = new NearbyRestaurantVO();
                restaurant.setRestaurantId(restaurantId);
                restaurant.setRestaurantName(restaurantName);
                restaurant.setLongitude(longitude);
                restaurant.setLatitude(latitude);

                redisTemplate.opsForValue().set(infoKey, restaurant);

                log.info("餐厅位置添加成功，ID: {}", restaurantId);
                return ApiResult.success("餐厅位置添加成功");
            } else {
                log.warn("餐厅位置添加失败，可能已存在，ID: {}", restaurantId);
                return ApiResult.success("餐厅位置更新成功");
            }

        } catch (Exception e) {
            log.error("添加餐厅位置失败: {}", e.getMessage(), e);
            return ApiResult.error("添加餐厅位置失败: " + e.getMessage());
        }
    }

    @Override
    public Result<String> batchAddRestaurantLocations(List<NearbyRestaurantVO> restaurants) {
        try {
            log.info("批量添加餐厅位置，数量: {}", restaurants.size());

            int successCount = 0;
            for (NearbyRestaurantVO restaurant : restaurants) {
                Result<String> result = addRestaurantLocation(
                        restaurant.getRestaurantId(),
                        restaurant.getLongitude(),
                        restaurant.getLatitude(),
                        restaurant.getRestaurantName());

                if (result.isSuccess()) {
                    successCount++;
                }
            }

            log.info("批量添加餐厅位置完成，成功: {}/{}", successCount, restaurants.size());
            return ApiResult.success(String.format("批量添加完成，成功 %d/%d 个餐厅",
                    successCount, restaurants.size()));

        } catch (Exception e) {
            log.error("批量添加餐厅位置失败: {}", e.getMessage(), e);
            return ApiResult.error("批量添加失败: " + e.getMessage());
        }
    }

    @Override
    public Result<List<NearbyRestaurantVO>> findNearbyRestaurants(Double longitude, Double latitude,
            Double radius, Integer limit) {
        try {
            log.info("搜索附近餐厅，中心点: ({}, {}), 半径: {}m, 限制: {}",
                    longitude, latitude, radius, limit);

            // 创建搜索中心点
            Point center = new Point(longitude, latitude);

            // 创建搜索距离（米）
            Distance distance = new Distance(radius / 1000.0, Metrics.KILOMETERS);

            // 创建搜索圆形区域
            Circle circle = new Circle(center, distance);

            // 设置搜索参数
            RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                    .newGeoRadiusArgs()
                    .includeDistance()
                    .includeCoordinates()
                    .sortAscending()
                    .limit(limit);

            // 执行搜索
            GeoResults<RedisGeoCommands.GeoLocation<Object>> results = redisTemplate.opsForGeo()
                    .radius(RESTAURANTS_GEO_KEY, circle, args);

            List<NearbyRestaurantVO> nearbyRestaurants = new ArrayList<>();

            if (results != null) {
                for (GeoResult<RedisGeoCommands.GeoLocation<Object>> result : results) {
                    RedisGeoCommands.GeoLocation<Object> location = result.getContent();
                    String member = location.getName().toString();

                    // 提取餐厅ID
                    if (member.startsWith("restaurant_")) {
                        Integer restaurantId = Integer.parseInt(member.substring(11));

                        // 获取餐厅详细信息
                        String infoKey = "restaurant:info:" + restaurantId;
                        Object restaurantInfo = redisTemplate.opsForValue().get(infoKey);

                        NearbyRestaurantVO restaurant;
                        if (restaurantInfo instanceof NearbyRestaurantVO) {
                            restaurant = (NearbyRestaurantVO) restaurantInfo;
                        } else {
                            // 如果没有详细信息，创建基本信息
                            restaurant = new NearbyRestaurantVO();
                            restaurant.setRestaurantId(restaurantId);
                            restaurant.setRestaurantName("餐厅_" + restaurantId);
                            restaurant.setLongitude(location.getPoint().getX());
                            restaurant.setLatitude(location.getPoint().getY());
                        }

                        // 设置距离
                        restaurant.setDistance(result.getDistance().getValue());

                        nearbyRestaurants.add(restaurant);
                    }
                }
            }

            log.info("搜索附近餐厅完成，找到 {} 个餐厅", nearbyRestaurants.size());
            return ApiResult.success(nearbyRestaurants);

        } catch (Exception e) {
            log.error("搜索附近餐厅失败: {}", e.getMessage(), e);
            return ApiResult.error("搜索失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Double> calculateDistance(Integer restaurantId1, Integer restaurantId2) {
        try {
            log.info("计算餐厅距离，餐厅1: {}, 餐厅2: {}", restaurantId1, restaurantId2);

            String member1 = "restaurant_" + restaurantId1;
            String member2 = "restaurant_" + restaurantId2;

            // 计算两点间距离
            Distance distance = redisTemplate.opsForGeo().distance(
                    RESTAURANTS_GEO_KEY, member1, member2, Metrics.KILOMETERS);

            if (distance != null) {
                double distanceValue = distance.getValue() * 1000; // 转换为米
                log.info("餐厅距离计算完成，距离: {} 米", distanceValue);
                return ApiResult.success(distanceValue);
            } else {
                log.warn("无法计算距离，可能餐厅不存在");
                return ApiResult.error("无法计算距离，请检查餐厅是否存在");
            }

        } catch (Exception e) {
            log.error("计算餐厅距离失败: {}", e.getMessage(), e);
            return ApiResult.error("计算距离失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Double[]> getRestaurantPosition(Integer restaurantId) {
        try {
            log.info("获取餐厅位置，餐厅ID: {}", restaurantId);

            String member = "restaurant_" + restaurantId;

            // 获取位置坐标
            List<Point> positions = redisTemplate.opsForGeo().position(RESTAURANTS_GEO_KEY, member);

            if (positions != null && !positions.isEmpty() && positions.get(0) != null) {
                Point point = positions.get(0);
                Double[] coordinates = { point.getX(), point.getY() }; // [经度, 纬度]

                log.info("获取餐厅位置成功，坐标: [{}, {}]", coordinates[0], coordinates[1]);
                return ApiResult.success(coordinates);
            } else {
                log.warn("餐厅位置不存在，ID: {}", restaurantId);
                return ApiResult.error("餐厅位置不存在");
            }

        } catch (Exception e) {
            log.error("获取餐厅位置失败: {}", e.getMessage(), e);
            return ApiResult.error("获取位置失败: " + e.getMessage());
        }
    }

    @Override
    public Result<String> removeRestaurantLocation(Integer restaurantId) {
        try {
            log.info("删除餐厅位置，餐厅ID: {}", restaurantId);

            String member = "restaurant_" + restaurantId;

            // 从GEO中删除
            Long result = redisTemplate.opsForGeo().remove(RESTAURANTS_GEO_KEY, member);

            // 删除详细信息
            String infoKey = "restaurant:info:" + restaurantId;
            redisTemplate.delete(infoKey);

            if (result != null && result > 0) {
                log.info("餐厅位置删除成功，ID: {}", restaurantId);
                return ApiResult.success("餐厅位置删除成功");
            } else {
                log.warn("餐厅位置不存在，ID: {}", restaurantId);
                return ApiResult.error("餐厅位置不存在");
            }

        } catch (Exception e) {
            log.error("删除餐厅位置失败: {}", e.getMessage(), e);
            return ApiResult.error("删除失败: " + e.getMessage());
        }
    }

    @Override
    public Result<List<NearbyRestaurantVO>> getRestaurantsInArea(Double centerLongitude, Double centerLatitude,
            Double width, Double height) {
        try {
            log.info("获取区域内餐厅，中心: ({}, {}), 宽: {}m, 高: {}m",
                    centerLongitude, centerLatitude, width, height);

            // 计算矩形区域的边界
            double halfWidth = width / 2;
            double halfHeight = height / 2;

            // 简化实现：使用圆形区域近似矩形区域
            double radius = Math.max(halfWidth, halfHeight);

            return findNearbyRestaurants(centerLongitude, centerLatitude, radius, 100);

        } catch (Exception e) {
            log.error("获取区域内餐厅失败: {}", e.getMessage(), e);
            return ApiResult.error("获取失败: " + e.getMessage());
        }
    }
}
