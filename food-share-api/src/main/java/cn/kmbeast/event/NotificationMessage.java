package cn.kmbeast.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知消息实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 通知ID
     */
    private String id;

    /**
     * 通知类型：comment-评论，upvote-点赞
     */
    private String type;

    /**
     * 发送者ID
     */
    private Integer senderId;

    /**
     * 发送者名称
     */
    private String senderName;

    /**
     * 发送者头像
     */
    private String senderAvatar;

    /**
     * 接收者ID
     */
    private Integer receiverId;

    /**
     * 内容ID（如美食ID）
     */
    private Integer contentId;

    /**
     * 内容类型
     */
    private String contentType;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 通知时间
     */
    private LocalDateTime createTime;

    /**
     * 是否已读
     */
    private Boolean isRead;

    /**
     * 相关数据，如评论ID
     */
    private String relatedData;
}