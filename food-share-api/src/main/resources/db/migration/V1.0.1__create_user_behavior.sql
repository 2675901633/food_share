USE `food_share`;

CREATE TABLE `user_behavior` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `gourmet_id` int NOT NULL COMMENT '美食ID',
  `behavior_type` varchar(20) NOT NULL COMMENT '行为类型：VIEW（浏览）、LIKE（点赞）、COMMENT（评论）、COLLECT（收藏）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '行为发生时间',
  `detail` text COMMENT '行为详情（如评论内容）',
  PRIMARY KEY (`id`),
  KEY `idx_user_gourmet` (`user_id`,`gourmet_id`),
  KEY `idx_gourmet_behavior` (`gourmet_id`,`behavior_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户行为表';
