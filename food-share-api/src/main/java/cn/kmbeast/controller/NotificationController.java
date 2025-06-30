package cn.kmbeast.controller;

import cn.kmbeast.aop.Protector;
import cn.kmbeast.context.LocalThreadHolder;
import cn.kmbeast.event.NotificationMessage;
import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import javax.annotation.Resource;

/**
 * 通知控制器
 */
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @Resource
    private NotificationService notificationService;

    /**
     * 获取当前用户的未读通知
     *
     * @param page 页码，从0开始
     * @param size 每页大小
     * @return 通知列表
     */
    @Protector
    @GetMapping("/unread")
    @ResponseBody
    public Result<Object> getUnreadNotifications(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Integer userId = LocalThreadHolder.getUserId();
        return notificationService.getUserUnreadNotifications(userId, page, size);
    }

    /**
     * 获取当前用户的所有通知
     *
     * @param page 页码，从0开始
     * @param size 每页大小
     * @return 通知列表
     */
    @Protector
    @GetMapping("/all")
    @ResponseBody
    public Result<Object> getAllNotifications(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Integer userId = LocalThreadHolder.getUserId();
        return notificationService.getUserAllNotifications(userId, page, size);
    }

    /**
     * 标记通知为已读
     *
     * @param notificationId 通知ID，如果为null则标记所有通知为已读
     * @return 操作结果
     */
    @Protector
    @PutMapping("/read")
    @ResponseBody
    public Result<Object> markAsRead(@RequestParam(required = false) String notificationId) {
        Integer userId = LocalThreadHolder.getUserId();
        return notificationService.markNotificationAsRead(userId, notificationId);
    }

    /**
     * 标记所有通知为已读
     *
     * @return 操作结果
     */
    @Protector
    @PutMapping("/read/all")
    @ResponseBody
    public Result<Object> markAllAsRead() {
        Integer userId = LocalThreadHolder.getUserId();
        return notificationService.markNotificationAsRead(userId, null);
    }

    /**
     * 获取广播通知
     *
     * @param page 页码，从0开始
     * @param size 每页大小
     * @return 广播通知列表
     */
    @GetMapping("/broadcast")
    @ResponseBody
    public Result<List<NotificationMessage>> getBroadcastNotifications(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return notificationService.getBroadcastNotifications(page, size);
    }

    /**
     * 清理错误格式的通知数据（管理员功能）
     *
     * @return 操作结果
     */
    @PostMapping("/admin/cleanup")
    @ResponseBody
    public Result<String> cleanupNotifications() {
        try {
            // 这里可以添加管理员权限检查
            // 清理广播通知
            notificationService.cleanupBroadcastNotifications();
            return ApiResult.success("通知数据清理完成");
        } catch (Exception e) {
            return ApiResult.error("清理失败: " + e.getMessage());
        }
    }
}