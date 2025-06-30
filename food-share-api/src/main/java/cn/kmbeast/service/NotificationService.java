package cn.kmbeast.service;

import cn.kmbeast.event.NotificationMessage;
import cn.kmbeast.pojo.api.Result;

import java.util.List;

/**
 * 通知服务接口
 */
public interface NotificationService {

    /**
     * 发送通知
     * 
     * @param notification 通知消息
     */
    void sendNotification(NotificationMessage notification);

    /**
     * 获取用户未读通知
     *
     * @param userId 用户ID
     * @param page   页码，从0开始
     * @param size   每页大小
     * @return 通知列表
     */
    Result<Object> getUserUnreadNotifications(Integer userId, Integer page, Integer size);

    /**
     * 获取广播通知
     *
     * @param page 页码
     * @param size 每页大小
     * @return 广播通知列表
     */
    Result<List<NotificationMessage>> getBroadcastNotifications(Integer page, Integer size);

    /**
     * 清理错误格式的广播通知数据
     */
    void cleanupBroadcastNotifications();

    /**
     * 获取用户所有通知
     * 
     * @param userId 用户ID
     * @param page   页码，从0开始
     * @param size   每页大小
     * @return 通知列表
     */
    Result<Object> getUserAllNotifications(Integer userId, Integer page, Integer size);

    /**
     * 标记通知为已读
     * 
     * @param userId         用户ID
     * @param notificationId 通知ID，如果为null则标记所有通知为已读
     * @return 操作结果
     */
    Result<Object> markNotificationAsRead(Integer userId, String notificationId);
}