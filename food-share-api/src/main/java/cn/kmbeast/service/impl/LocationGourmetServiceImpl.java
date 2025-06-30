package cn.kmbeast.service.impl;

import cn.kmbeast.mapper.GourmetMapper;
import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.extend.GourmetQueryDto;
import cn.kmbeast.pojo.em.AuditEnum;
import cn.kmbeast.pojo.em.PublishEnum;
import cn.kmbeast.pojo.entity.Gourmet;
import cn.kmbeast.pojo.vo.GourmetListVO;
import cn.kmbeast.pojo.vo.GourmetVO;
import cn.kmbeast.service.LocationGourmetService;
import cn.kmbeast.utils.RedisUtil;
import cn.kmbeast.utils.TextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于地理位置的美食推荐服务实现类
 */
@Slf4j
@Service
public class LocationGourmetServiceImpl implements LocationGourmetService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private GourmetMapper gourmetMapper;

    /**
     * Redis GEO 键名
     */
    private static final String GOURMET_GEO_KEY = "gourmet:locations";

    /**
     * 获取附近的美食推荐
     */
    @Override
    public Result<List<GourmetListVO>> getNearbyGourmets(Double longitude, Double latitude, Integer radius,
            Integer limit) {
        try {
            // 检查Redis中是否有地理位置数据
            Long geoCount = redisTemplate.opsForZSet().zCard(GOURMET_GEO_KEY);
            if (geoCount == null || geoCount == 0) {
                log.warn("Redis中没有地理位置数据，返回空结果。请先初始化演示数据。");
                return ApiResult.success(new ArrayList<>());
            }

            // 使用Redis GEO命令查找附近的美食
            Point center = new Point(longitude, latitude);
            Distance distance = new Distance(radius, Metrics.NEUTRAL);
            Circle circle = new Circle(center, distance);

            // 执行GEO搜索
            GeoResults<RedisGeoCommands.GeoLocation<Object>> results = redisTemplate.opsForGeo()
                    .radius(GOURMET_GEO_KEY, circle, RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                            .includeDistance()
                            .includeCoordinates()
                            .sortAscending()
                            .limit(limit));

            if (results == null || results.getContent().isEmpty()) {
                log.info("在指定范围内没有找到美食，中心点: ({}, {}), 半径: {}米", longitude, latitude, radius);
                return ApiResult.success(new ArrayList<>());
            }

            // 提取美食ID列表
            List<Integer> gourmetIds = results.getContent().stream()
                    .map(result -> Integer.parseInt(result.getContent().getName().toString()))
                    .collect(Collectors.toList());

            // 从数据库获取美食详细信息
            List<GourmetVO> gourmetVOs = gourmetMapper.queryByIds(gourmetIds);

            // 转换为GourmetListVO并添加距离信息
            List<GourmetListVO> gourmetListVOs = new ArrayList<>();
            Map<Integer, Double> distanceMap = new HashMap<>();

            // 构建距离映射
            for (GeoResult<RedisGeoCommands.GeoLocation<Object>> result : results.getContent()) {
                Integer gourmetId = Integer.parseInt(result.getContent().getName().toString());
                Double dist = result.getDistance().getValue();
                distanceMap.put(gourmetId, dist);
            }

            // 转换VO并添加距离信息
            for (GourmetVO gourmetVO : gourmetVOs) {
                Double dist = distanceMap.get(gourmetVO.getId());
                GourmetListVO listVO = new GourmetListVO(
                        gourmetVO.getId(),
                        gourmetVO.getTitle(),
                        gourmetVO.getCover(),
                        TextUtil.parseText(gourmetVO.getContent(), 180),
                        gourmetVO.getUserName(),
                        gourmetVO.getUserAvatar(),
                        gourmetVO.getViewCount(),
                        gourmetVO.getUpvoteCount(),
                        gourmetVO.getSaveCount(),
                        gourmetVO.getRating(),
                        gourmetVO.getCreateTime(),
                        gourmetVO.getLocationName(),
                        dist);
                gourmetListVOs.add(listVO);
            }

            // 按距离排序
            gourmetListVOs.sort(Comparator.comparing(GourmetListVO::getDistance));

            log.info("查找附近美食成功，中心点: ({}, {}), 半径: {}米, 找到: {}个",
                    longitude, latitude, radius, gourmetListVOs.size());

            return ApiResult.success(gourmetListVOs);

        } catch (Exception e) {
            log.error("查找附近美食失败: {}", e.getMessage(), e);
            return ApiResult.error("查找附近美食失败");
        }
    }

    /**
     * 添加美食的地理位置信息
     */
    @Override
    public Result<String> addGourmetLocation(Integer gourmetId, Double longitude, Double latitude,
            String locationName) {
        try {
            // 更新数据库中的位置信息
            Gourmet gourmet = new Gourmet();
            gourmet.setId(gourmetId);
            gourmet.setLongitude(longitude);
            gourmet.setLatitude(latitude);
            gourmet.setLocationName(locationName);

            gourmetMapper.update(gourmet);

            // 添加到Redis GEO
            Point point = new Point(longitude, latitude);
            redisTemplate.opsForGeo().add(GOURMET_GEO_KEY, point, gourmetId.toString());

            log.info("添加美食位置信息成功，美食ID: {}, 位置: ({}, {}), 地址: {}",
                    gourmetId, longitude, latitude, locationName);

            return ApiResult.success("位置信息添加成功");

        } catch (Exception e) {
            log.error("添加美食位置信息失败: {}", e.getMessage(), e);
            return ApiResult.error("添加位置信息失败");
        }
    }

    /**
     * 批量初始化美食位置数据（用于演示）
     */
    @Override
    public Result<String> initDemoLocations() {
        try {
            // 清空现有的地理位置数据
            redisTemplate.delete(GOURMET_GEO_KEY);

            // 获取所有已发布的美食
            GourmetQueryDto queryDto = new GourmetQueryDto();
            queryDto.setIsPublish(PublishEnum.OK_AUDIT.getFlag());
            queryDto.setIsAudit(AuditEnum.OK_AUDIT.getFlag());
            queryDto.setCurrent(1);
            queryDto.setSize(100); // 限制100个用于演示

            List<GourmetVO> gourmets = gourmetMapper.query(queryDto);

            if (gourmets.isEmpty()) {
                return ApiResult.error("没有找到可初始化的美食数据");
            }

            // 模拟一些城市的坐标点（以北京为中心）
            List<LocationPoint> demoLocations = Arrays.asList(
                    new LocationPoint(116.404, 39.915, "北京市朝阳区"),
                    new LocationPoint(116.383, 39.911, "北京市东城区"),
                    new LocationPoint(116.366, 39.928, "北京市西城区"),
                    new LocationPoint(116.454, 39.919, "北京市海淀区"),
                    new LocationPoint(116.368, 39.856, "北京市丰台区"),
                    new LocationPoint(116.286, 39.906, "北京市石景山区"),
                    new LocationPoint(116.653, 39.902, "北京市通州区"),
                    new LocationPoint(116.338, 40.077, "北京市昌平区"));

            Random random = new Random();
            int updatedCount = 0;

            for (GourmetVO gourmet : gourmets) {
                // 随机选择一个位置点
                LocationPoint location = demoLocations.get(random.nextInt(demoLocations.size()));

                // 在选定位置附近随机偏移（±0.01度，约1公里范围）
                double longitude = location.longitude + (random.nextDouble() - 0.5) * 0.02;
                double latitude = location.latitude + (random.nextDouble() - 0.5) * 0.02;

                // 更新数据库
                Gourmet updateGourmet = new Gourmet();
                updateGourmet.setId(gourmet.getId());
                updateGourmet.setLongitude(longitude);
                updateGourmet.setLatitude(latitude);
                updateGourmet.setLocationName(location.name);

                gourmetMapper.update(updateGourmet);

                // 添加到Redis GEO
                Point point = new Point(longitude, latitude);
                redisTemplate.opsForGeo().add(GOURMET_GEO_KEY, point, gourmet.getId().toString());

                updatedCount++;
            }

            log.info("批量初始化美食位置数据完成，共处理: {}个美食", updatedCount);
            return ApiResult.success(String.format("成功初始化%d个美食的位置数据", updatedCount));

        } catch (Exception e) {
            log.error("批量初始化美食位置数据失败: {}", e.getMessage(), e);
            return ApiResult.error("初始化位置数据失败");
        }
    }

    /**
     * 获取同城热门美食
     */
    @Override
    public Result<List<GourmetListVO>> getCityPopularGourmets(Double longitude, Double latitude, Integer cityRadius,
            Integer limit) {
        try {
            // 先获取同城范围内的所有美食
            Result<List<GourmetListVO>> nearbyResult = getNearbyGourmets(longitude, latitude, cityRadius, limit * 3);

            if (!nearbyResult.isSuccess() || nearbyResult.getData() == null || nearbyResult.getData().isEmpty()) {
                return ApiResult.success(new ArrayList<>());
            }

            List<GourmetListVO> nearbyGourmets = nearbyResult.getData();

            // 按热度排序（综合浏览量、点赞量、收藏量）
            nearbyGourmets.sort((a, b) -> {
                int scoreA = (a.getViewCount() != null ? a.getViewCount() : 0) * 1 +
                        (a.getUpvoteCount() != null ? a.getUpvoteCount() : 0) * 3 +
                        (a.getSaveCount() != null ? a.getSaveCount() : 0) * 5;
                int scoreB = (b.getViewCount() != null ? b.getViewCount() : 0) * 1 +
                        (b.getUpvoteCount() != null ? b.getUpvoteCount() : 0) * 3 +
                        (b.getSaveCount() != null ? b.getSaveCount() : 0) * 5;
                return Integer.compare(scoreB, scoreA); // 降序
            });

            // 限制返回数量
            List<GourmetListVO> result = nearbyGourmets.stream()
                    .limit(limit)
                    .collect(Collectors.toList());

            log.info("获取同城热门美食成功，位置: ({}, {}), 范围: {}米, 返回: {}个",
                    longitude, latitude, cityRadius, result.size());

            return ApiResult.success(result);

        } catch (Exception e) {
            log.error("获取同城热门美食失败: {}", e.getMessage(), e);
            return ApiResult.error("获取同城热门美食失败");
        }
    }

    /**
     * 位置点辅助类
     */
    private static class LocationPoint {
        double longitude;
        double latitude;
        String name;

        LocationPoint(double longitude, double latitude, String name) {
            this.longitude = longitude;
            this.latitude = latitude;
            this.name = name;
        }
    }
}
