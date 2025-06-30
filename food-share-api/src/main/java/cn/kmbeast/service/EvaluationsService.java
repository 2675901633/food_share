package cn.kmbeast.service;

import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.base.QueryDto;
import cn.kmbeast.pojo.dto.query.extend.EvaluationsQueryDto;
import cn.kmbeast.pojo.entity.Evaluations;

import java.util.List;

/**
 * 评论服务接口
 */
public interface EvaluationsService {

    Result<Object> insert(Evaluations evaluations);

    Result<Object> list(Integer contentId, String contentType);

    Result<Object> query(EvaluationsQueryDto evaluationsQueryDto);

    Result<Object> batchDelete(List<Integer> ids);

    Result<String> delete(Integer id);

    Result<Void> update(Evaluations evaluations);

    /**
     * 获取最新评论（从Redis缓存中获取）
     * 
     * @param contentId 内容ID
     * @param page      页码，从0开始
     * @param size      每页大小
     * @return 包含分页后的评论数据
     */
    Result<Object> getLatestComments(Integer contentId, Integer page, Integer size);

}
