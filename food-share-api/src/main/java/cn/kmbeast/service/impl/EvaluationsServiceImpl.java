package cn.kmbeast.service.impl;

import cn.kmbeast.context.LocalThreadHolder;
import cn.kmbeast.event.NotificationMessage;
import cn.kmbeast.mapper.EvaluationsMapper;
import cn.kmbeast.mapper.GourmetMapper;
import cn.kmbeast.mapper.UserMapper;
import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.api.PageResult;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.extend.EvaluationsQueryDto;
import cn.kmbeast.pojo.entity.Evaluations;
import cn.kmbeast.pojo.entity.Gourmet;
import cn.kmbeast.pojo.entity.User;
import cn.kmbeast.pojo.vo.CommentChildVO;
import cn.kmbeast.pojo.vo.CommentParentVO;
import cn.kmbeast.pojo.vo.EvaluationsVO;
import cn.kmbeast.pojo.vo.GourmetVO;
import cn.kmbeast.service.EvaluationsService;
import cn.kmbeast.service.NotificationService;
import cn.kmbeast.utils.RedisUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 评论服务实现类
 */
@Service
public class EvaluationsServiceImpl implements EvaluationsService {

    @Resource
    private EvaluationsMapper evaluationsMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private GourmetMapper gourmetMapper;
    @Resource
    private RedisUtil redisUtil;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private NotificationService notificationService;

    /**
     * 评论
     *
     * @return Result<String>
     */
    @Override
    public Result<Object> insert(Evaluations evaluations) {
        // 设置评论者ID
        Integer commenterId = LocalThreadHolder.getUserId();
        evaluations.setCommenterId(commenterId);

        // 检查用户是否被禁言
        User queryConditionEntity = User.builder().id(commenterId).build();
        User commenter = userMapper.getByActive(queryConditionEntity);
        if (commenter.getIsWord()) {
            return ApiResult.error("账户已被禁言");
        }

        // 设置评论时间
        evaluations.setCreateTime(LocalDateTime.now());

        // 保存到数据库
        evaluationsMapper.save(evaluations);

        // 保存到Redis List
        try {
            // 构建评论的Redis键
            String redisKey = "gourmet:" + evaluations.getContentId() + ":comments";

            // 将评论对象转为JSON字符串
            String commentJson = objectMapper.writeValueAsString(evaluations);

            // 使用LPUSH将评论添加到List头部，这样最新的评论总是在前面
            redisUtil.lSet(redisKey, commentJson);

            // 设置过期时间，例如7天
            redisUtil.expire(redisKey, 7 * 24 * 60 * 60);

        } catch (JsonProcessingException e) {
            // 记录异常，但不影响主流程
            e.printStackTrace();
        }

        // 发送通知
        sendNotification(evaluations, commenter);

        return ApiResult.success("评论成功");
    }

    /**
     * 发送评论通知
     * 
     * @param evaluations 评论信息
     * @param commenter   评论者信息
     */
    private void sendNotification(Evaluations evaluations, User commenter) {
        try {
            Integer receiverId = null;
            String contentTitle = "";

            // 如果是回复其他评论
            if (evaluations.getParentId() != null) {
                // 获取父评论信息，通知被回复的用户
                // 由于没有直接的getById方法，这里使用查询方式获取评论信息
                EvaluationsQueryDto queryDto = new EvaluationsQueryDto();
                queryDto.setId(evaluations.getParentId());
                List<CommentChildVO> parentComments = evaluationsMapper.query(queryDto);
                if (!parentComments.isEmpty()) {
                    receiverId = parentComments.get(0).getUserId();
                }
            } else if ("gourmet".equals(evaluations.getContentType())) {
                // 如果是评论美食，通知美食作者
                GourmetVO gourmet = gourmetMapper.queryById(evaluations.getContentId());
                if (gourmet != null) {
                    receiverId = gourmet.getUserId();
                    contentTitle = gourmet.getTitle();
                }
            }

            // 如果找到了接收者，并且接收者不是评论者自己
            if (receiverId != null && !receiverId.equals(commenter.getId())) {
                // 创建通知消息
                NotificationMessage notification = NotificationMessage.builder()
                        .type("comment")
                        .senderId(commenter.getId())
                        .senderName(commenter.getUserName())
                        .senderAvatar(commenter.getUserAvatar())
                        .receiverId(receiverId)
                        .contentId(evaluations.getContentId())
                        .contentType(evaluations.getContentType())
                        .content(commenter.getUserName() + " 评论了你的" +
                                ("gourmet".equals(evaluations.getContentType()) ? "美食" : "评论") +
                                (contentTitle.isEmpty() ? "" : "「" + contentTitle + "」") +
                                ": " + evaluations.getContent())
                        .relatedData(evaluations.getId().toString())
                        .createTime(LocalDateTime.now())
                        .isRead(false)
                        .build();

                // 发送通知
                notificationService.sendNotification(notification);
            }
        } catch (Exception e) {
            // 记录异常，但不影响主流程
            e.printStackTrace();
        }
    }

    /**
     * 查询全部评论
     *
     * @return Result<String>
     */
    @Override
    public Result<Object> list(Integer contentId, String contentType) {
        List<CommentParentVO> parentComments = evaluationsMapper.getParentComments(contentId, contentType);
        setUpvoteFlag(parentComments);
        Integer count = evaluationsMapper.totalCount(contentId, contentType);
        return ApiResult.success(new EvaluationsVO(count, parentComments));
    }

    /**
     * 获取最新评论（从Redis缓存中获取）
     *
     * @param contentId 内容ID
     * @param page      页码，从0开始
     * @param size      每页大小
     * @return 包含分页后的评论数据
     */
    @Override
    public Result<Object> getLatestComments(Integer contentId, Integer page, Integer size) {
        // 构建Redis键
        String redisKey = "gourmet:" + contentId + ":comments";

        // 检查键是否存在
        if (!redisUtil.hasKey(redisKey)) {
            return ApiResult.success(new HashMap<String, Object>() {
                {
                    put("total", 0);
                    put("comments", Collections.emptyList());
                }
            });
        }

        // 获取列表总长度
        long total = redisUtil.lGetListSize(redisKey);

        // 计算分页起始和结束索引
        long start = (long) page * size;
        long end = start + size - 1;

        // 如果起始索引超出范围，返回空结果
        if (start >= total) {
            return ApiResult.success(new HashMap<String, Object>() {
                {
                    put("total", total);
                    put("comments", Collections.emptyList());
                }
            });
        }

        // 从Redis获取指定范围的评论
        List<Object> commentJsonList = redisUtil.lGet(redisKey, start, end);

        // 解析JSON并转换为评论对象
        List<Evaluations> comments = new ArrayList<>();
        for (Object jsonObj : commentJsonList) {
            try {
                String json = jsonObj.toString();
                Evaluations comment = objectMapper.readValue(json, Evaluations.class);
                comments.add(comment);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 返回结果
        return ApiResult.success(new HashMap<String, Object>() {
            {
                put("total", total);
                put("comments", comments);
            }
        });
    }

    /**
     * 设置点赞状态
     *
     * @param parentComments 评论数据列表
     */
    private void setUpvoteFlag(List<CommentParentVO> parentComments) {
        String userId = LocalThreadHolder.getUserId().toString(); // 预先获取用户ID
        parentComments.forEach(parentComment -> {
            parentComment.setUpvoteFlag(isUserUpvote(parentComment.getUpvoteList(), userId));
            parentComment.setUpvoteCount(countVotes(parentComment.getUpvoteList()));
            Optional.ofNullable(parentComment.getCommentChildVOS())
                    .orElse(Collections.emptyList())
                    .forEach(child -> {
                        child.setUpvoteFlag(isUserUpvote(child.getUpvoteList(), userId));
                        child.setUpvoteCount(countVotes(child.getUpvoteList()));
                    });
        });
    }

    /**
     * 判断用户是否已点赞
     *
     * @param voteStr 点赞用户ID字符串（逗号分隔）
     * @param userId  用户ID
     * @return 是否已点赞
     */
    private boolean isUserUpvote(String voteStr, String userId) {
        return Optional.ofNullable(voteStr)
                .map(s -> Arrays.asList(s.split(",")))
                .orElse(Collections.emptyList())
                .contains(userId);
    }

    /**
     * 计算点赞数
     *
     * @param voteStr 点赞用户ID字符串（逗号分隔）
     * @return 点赞数
     */
    private int countVotes(String voteStr) {
        return Optional.ofNullable(voteStr)
                .map(s -> s.split(",").length)
                .orElse(0);
    }

    /**
     * 分页查询评论
     *
     * @return Result<String>
     */
    @Override
    public Result<Object> query(EvaluationsQueryDto evaluationsQueryDto) {
        List<CommentChildVO> list = evaluationsMapper.query(evaluationsQueryDto);
        Integer totalPage = evaluationsMapper.queryCount(evaluationsQueryDto);
        return PageResult.success(list, totalPage);
    }

    /**
     * 批量删除评论数据
     *
     * @return Result<String>
     */
    @Override
    public Result<Object> batchDelete(List<Integer> ids) {
        evaluationsMapper.batchDelete(ids);
        return ApiResult.success();
    }

    /**
     * 评论删除
     *
     * @return Result<String>
     */
    @Override
    public Result<String> delete(Integer id) {
        ArrayList<Integer> ids = new ArrayList<>();
        ids.add(id);
        evaluationsMapper.batchDelete(ids);
        return ApiResult.success();
    }

    /**
     * 评论修改
     *
     * @return Result<String>
     */
    @Override
    public Result<Void> update(Evaluations evaluations) {
        // 更新评论
        evaluationsMapper.update(evaluations);

        // 如果是点赞操作，发送点赞通知
        if (evaluations.getUpvoteList() != null) {
            sendUpvoteNotification(evaluations);
        }

        return ApiResult.success();
    }

    /**
     * 发送点赞通知
     * 
     * @param evaluations 评论信息
     */
    private void sendUpvoteNotification(Evaluations evaluations) {
        try {
            // 获取当前用户（点赞者）
            Integer upvoterId = LocalThreadHolder.getUserId();
            User upvoter = userMapper.getByActive(User.builder().id(upvoterId).build());

            // 如果点赞者不是评论者本人，则发送通知
            if (!upvoterId.equals(evaluations.getCommenterId())) {
                // 创建通知消息
                NotificationMessage notification = NotificationMessage.builder()
                        .type("upvote")
                        .senderId(upvoter.getId())
                        .senderName(upvoter.getUserName())
                        .senderAvatar(upvoter.getUserAvatar())
                        .receiverId(evaluations.getCommenterId())
                        .contentId(evaluations.getContentId())
                        .contentType(evaluations.getContentType())
                        .content(upvoter.getUserName() + " 赞了你的评论: " +
                                (evaluations.getContent().length() > 20
                                        ? evaluations.getContent().substring(0, 20) + "..."
                                        : evaluations.getContent()))
                        .relatedData(evaluations.getId().toString())
                        .createTime(LocalDateTime.now())
                        .isRead(false)
                        .build();

                // 发送通知
                notificationService.sendNotification(notification);
            }
        } catch (Exception e) {
            // 记录异常，但不影响主流程
            e.printStackTrace();
        }
    }
}
