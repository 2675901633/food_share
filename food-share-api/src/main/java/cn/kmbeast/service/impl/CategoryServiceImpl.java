package cn.kmbeast.service.impl;

import cn.kmbeast.aop.CacheableCategory;
import cn.kmbeast.event.CategoryChangeEvent;
import cn.kmbeast.event.OperationType;
import cn.kmbeast.mapper.CategoryMapper;
import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.extend.CategoryQueryDto;
import cn.kmbeast.pojo.entity.Category;
import cn.kmbeast.service.CategoryService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 美食类别业务逻辑接口实现类
 */
@Service
public class CategoryServiceImpl implements CategoryService {

    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    /**
     * 新增
     *
     * @param category 实体
     * @return Result<String> 通用的响应类
     */
    @Override
    public Result<String> save(Category category) {
        categoryMapper.save(category);
        // 发布分类变更事件，触发缓存更新
        eventPublisher.publishEvent(new CategoryChangeEvent(this, category.getId(), OperationType.CREATE));
        return ApiResult.success();
    }

    /**
     * 修改
     *
     * @param category 实体
     * @return Result<String> 通用的响应类
     */
    @Override
    public Result<String> update(Category category) {
        categoryMapper.update(category);
        // 发布分类变更事件，触发缓存更新
        eventPublisher.publishEvent(new CategoryChangeEvent(this, category.getId(), OperationType.UPDATE));
        return ApiResult.success();
    }

    /**
     * 删除
     *
     * @param ids ID列表
     * @return Result<String> 通用的响应类
     */
    @Override
    public Result<String> batchDelete(List<Integer> ids) {
        categoryMapper.batchDelete(ids);
        // 发布分类变更事件，触发缓存更新
        for (Integer id : ids) {
            eventPublisher.publishEvent(new CategoryChangeEvent(this, id, OperationType.DELETE));
        }
        return ApiResult.success();
    }

    /**
     * 查询
     * 使用CacheableCategory注解添加缓存功能
     *
     * @param categoryQueryDto 查询参数实体
     * @return Result<List < Category>> 通用的响应类
     */
    @Override
    @CacheableCategory
    public Result<List<Category>> query(CategoryQueryDto categoryQueryDto) {
        // 数据
        List<Category> categoryList = categoryMapper.query(categoryQueryDto);
        // 数据量
        Integer totalCount = categoryMapper.queryCount(categoryQueryDto);
        return ApiResult.success(categoryList, totalCount);
    }
}
