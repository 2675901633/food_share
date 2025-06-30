-- 为gourmet表添加位置字段
-- 执行时间：2025-06-29

-- 添加经度字段
ALTER TABLE gourmet ADD COLUMN longitude DOUBLE DEFAULT NULL COMMENT '发布位置-经度';

-- 添加纬度字段  
ALTER TABLE gourmet ADD COLUMN latitude DOUBLE DEFAULT NULL COMMENT '发布位置-纬度';

-- 添加位置名称字段
ALTER TABLE gourmet ADD COLUMN location_name VARCHAR(255) DEFAULT NULL COMMENT '发布位置-地址描述';

-- 创建地理位置索引（如果MySQL版本支持）
-- ALTER TABLE gourmet ADD SPATIAL INDEX idx_location (POINT(longitude, latitude));

-- 验证字段添加成功
-- SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT, COLUMN_COMMENT 
-- FROM INFORMATION_SCHEMA.COLUMNS 
-- WHERE TABLE_SCHEMA = 'food_share' AND TABLE_NAME = 'gourmet' 
-- AND COLUMN_NAME IN ('longitude', 'latitude', 'location_name');
