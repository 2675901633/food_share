package cn.kmbeast.service.impl;

import cn.kmbeast.event.NotificationMessage;
import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.service.NotificationService;
import cn.kmbeast.utils.RedisUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * 通知服务实现类
 */
@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 通知频道名称
     */
    private static final String NOTIFICATION_CHANNEL = "notification:channel";

    /**
     * 用户通知队列前缀
     */
    private static final String USER_NOTIFICATION_KEY_PREFIX = "user:";
    private static final String USER_NOTIFICATION_KEY_SUFFIX = ":notifications";

    /**
     * 发送通知
     * 
     * @param notification 通知消息
     */
    @Override
    public void sendNotification(NotificationMessage notification) {
        try {
            // 生成唯一ID
            if (notification.getId() == null) {
                notification.setId(UUID.randomUUID().toString());
            }

            // 设置创建时间
            if (notification.getCreateTime() == null) {
                notification.setCreateTime(java.time.LocalDateTime.now());
            }

            // 设置未读状态
            if (notification.getIsRead() == null) {
                notification.setIsRead(false);
            }

            // 转换为JSON
            String notificationJson = objectMapper.writeValueAsString(notification);

            // 发布通知到Redis频道（实时通知）
            redisTemplate.convertAndSend(NOTIFICATION_CHANNEL, notificationJson);

            // 处理离线消息存储
            if (notification.getReceiverId() == 0) {
                // 广播消息：保存到全局广播队列
                String broadcastKey = "broadcast:notifications";
                redisUtil.lSet(broadcastKey, notificationJson);
                redisUtil.expire(broadcastKey, 7 * 24 * 60 * 60); // 7天过期

                log.info("广播通知已发送到频道和全局队列: {}", notification.getContent());
            } else {
                // 单用户消息：保存到用户的通知队列
                String userNotificationKey = USER_NOTIFICATION_KEY_PREFIX + notification.getReceiverId()
                        + USER_NOTIFICATION_KEY_SUFFIX;
                redisUtil.lSet(userNotificationKey, notificationJson);

                // 设置过期时间，例如30天
                if (!redisUtil.hasKey(userNotificationKey)) {
                    redisUtil.expire(userNotificationKey, 30 * 24 * 60 * 60);
                }

                log.info("用户通知已发送: 用户ID={}, 内容={}", notification.getReceiverId(), notification.getContent());
            }

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取用户未读通知
     * 
     * @param userId 用户ID
     * @param page   页码，从0开始
     * @param size   每页大小
     * @return 通知列表
     */
    @Override
    public Result<Object> getUserUnreadNotifications(Integer userId, Integer page, Integer size) {
        String userNotificationKey = USER_NOTIFICATION_KEY_PREFIX + userId + USER_NOTIFICATION_KEY_SUFFIX;

        return getNotifications(userNotificationKey, page, size, true);
    }

    /**
     * 获取用户所有通知
     * 
     * @param userId 用户ID
     * @param page   页码，从0开始
     * @param size   每页大小
     * @return 通知列表
     */
    @Override
    public Result<Object> getUserAllNotifications(Integer userId, Integer page, Integer size) {
        String userNotificationKey = USER_NOTIFICATION_KEY_PREFIX + userId + USER_NOTIFICATION_KEY_SUFFIX;

        return getNotifications(userNotificationKey, page, size, false);
    }

    /**
     * 获取通知列表
     * 
     * @param key        Redis键
     * @param page       页码
     * @param size       每页大小
     * @param unreadOnly 是否只获取未读通知
     * @return 通知列表
     */
    private Result<Object> getNotifications(String key, Integer page, Integer size, boolean unreadOnly) {
        // 检查键是否存在
        if (!redisUtil.hasKey(key)) {
            return ApiResult.success(new HashMap<String, Object>() {
                {
                    put("total", 0);
                    put("notifications", Collections.emptyList());
                }
            });
        }

        // 获取列表总长度
        long total = redisUtil.lGetListSize(key);

        // 先获取所有通知
        List<Object> allNotifications = redisUtil.lGet(key, 0, -1);

        // 过滤并分页
        List<NotificationMessage> filteredNotifications = new ArrayList<>();
        int count = 0;

        for (Object obj : allNotifications) {
            try {
                String json = obj.toString();

                // 检查是否是数组格式
                if (json.trim().startsWith("[")) {
                    // 如果是数组，解析为数组然后取第一个元素
                    NotificationMessage[] notifications = objectMapper.readValue(json, NotificationMessage[].class);
                    if (notifications.length > 0) {
                        NotificationMessage notification = notifications[0];
                        if (!unreadOnly || !notification.getIsRead()) {
                            filteredNotifications.add(notification);
                        }
                    }
                } else {
                    // 如果是单个对象，直接解析
                    NotificationMessage notification = objectMapper.readValue(json, NotificationMessage.class);
                    if (!unreadOnly || !notification.getIsRead()) {
                        filteredNotifications.add(notification);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse notification JSON: {}", obj.toString(), e);
            }
        }

        // 计算总数
        int filteredTotal = filteredNotifications.size();

        // 分页
        int start = page * size;
        int end = Math.min(start + size, filteredTotal);

        // 如果起始索引超出范围，返回空结果
        if (start >= filteredTotal) {
            return ApiResult.success(new HashMap<String, Object>() {
                {
                    put("total", filteredTotal);
                    put("notifications", Collections.emptyList());
                }
            });
        }

        // 返回分页后的结果
        List<NotificationMessage> pagedNotifications = filteredNotifications.subList(start, end);

        return ApiResult.success(new HashMap<String, Object>() {
            {
                put("total", filteredTotal);
                put("notifications", pagedNotifications);
            }
        });
    }

    /**
     * 标记通知为已读
     * 
     * @param userId         用户ID
     * @param notificationId 通知ID，如果为null则标记所有通知为已读
     * @return 操作结果
     */
    @Override
    public Result<Object> markNotificationAsRead(Integer userId, String notificationId) {
        String userNotificationKey = USER_NOTIFICATION_KEY_PREFIX + userId + USER_NOTIFICATION_KEY_SUFFIX;

        // 检查键是否存在
        if (!redisUtil.hasKey(userNotificationKey)) {
            return ApiResult.error("用户没有通知");
        }

        // 获取所有通知
        List<Object> allNotifications = redisUtil.lGet(userNotificationKey, 0, -1);
        List<String> updatedNotifications = new ArrayList<>();

        for (Object obj : allNotifications) {
            try {
                String json = obj.toString();
                NotificationMessage notification = objectMapper.readValue(json, NotificationMessage.class);

                // 如果notificationId为null，则标记所有通知为已读
                // 或者如果notificationId匹配，则标记该通知为已读
                if (notificationId == null || notification.getId().equals(notificationId)) {
                    notification.setIsRead(true);
                }

                // 将更新后的通知转换回JSON
                updatedNotifications.add(objectMapper.writeValueAsString(notification));

            } catch (Exception e) {
                e.printStackTrace();
                // 如果解析失败，保留原始JSON
                updatedNotifications.add(obj.toString());
            }
        }

        // 删除旧的通知列表
        redisUtil.del(userNotificationKey);

        // 添加更新后的通知列表
        if (!updatedNotifications.isEmpty()) {
            redisUtil.lSet(userNotificationKey, updatedNotifications);
            // 恢复过期时间
            redisUtil.expire(userNotificationKey, 30 * 24 * 60 * 60);
        }

        return ApiResult.success("标记已读成功");
    }

    /**
     * 获取广播通知
     */
    @Override
    public Result<List<NotificationMessage>> getBroadcastNotifications(Integer page, Integer size) {
        try {
            String broadcastKey = "broadcast:notifications";

            // 检查广播队列是否存在
            if (!redisUtil.hasKey(broadcastKey)) {
                return ApiResult.success(new ArrayList<>());
            }

            // 获取广播通知列表
            List<Object> broadcastList = redisUtil.lGet(broadcastKey, 0, -1);

            List<NotificationMessage> notifications = new ArrayList<>();

            for (Object obj : broadcastList) {
                try {
                    if (obj instanceof String) {
                        String json = (String) obj;

                        // 检查是否是数组格式
                        if (json.trim().startsWith("[")) {
                            // 如果是数组，解析为数组然后取第一个元素
                            NotificationMessage[] notificationArray = objectMapper.readValue(json,
                                    NotificationMessage[].class);
                            if (notificationArray.length > 0) {
                                notifications.add(notificationArray[0]);
                            }
                        } else {
                            // 如果是单个对象，直接解析
                            NotificationMessage notification = objectMapper.readValue(json, NotificationMessage.class);
                            notifications.add(notification);
                        }
                    }
                } catch (JsonProcessingException e) {
                    log.warn("解析广播通知失败: {}", e.getMessage());
                }
            }

            // 按时间倒序排序
            notifications.sort((a, b) -> {
                if (a.getCreateTime() == null || b.getCreateTime() == null) {
                    return 0;
                }
                return b.getCreateTime().compareTo(a.getCreateTime());
            });

            // 分页处理
            int start = page * size;
            int end = Math.min(start + size, notifications.size());

            if (start >= notifications.size()) {
                return ApiResult.success(new ArrayList<>());
            }

            List<NotificationMessage> result = notifications.subList(start, end);

            log.info("获取广播通知成功，页码: {}, 大小: {}, 返回数量: {}", page, size, result.size());
            return ApiResult.success(result);

        } catch (Exception e) {
            log.error("获取广播通知失败: {}", e.getMessage(), e);
            return ApiResult.error("获取广播通知失败");
        }
    }

    /**
     * 清理错误格式的广播通知数据
     */
    @Override
    public void cleanupBroadcastNotifications() {
        try {
            String broadcastKey = "broadcast:notifications";

            if (!redisUtil.hasKey(broadcastKey)) {
                log.info("广播通知队列不存在，无需清理");
                return;
            }

            // 删除整个广播通知队列
            redisUtil.del(broadcastKey);
            log.info("已清理广播通知队列: {}", broadcastKey);

            // 也可以清理用户通知队列中的错误数据
            // 这里可以根据需要添加更多清理逻辑

        } catch (Exception e) {
            log.error("清理广播通知失败: {}", e.getMessage(), e);
            throw new RuntimeException("清理失败: " + e.getMessage());
        }
    }
}