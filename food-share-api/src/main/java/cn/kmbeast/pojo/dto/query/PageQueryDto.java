package cn.kmbeast.pojo.dto.query;

import lombok.Data;

/**
 * 分页查询DTO基类
 */
@Data
public class PageQueryDto {

    /**
     * 页码
     */
    private Integer current = 1;

    /**
     * 每页大小
     */
    private Integer size = 10;
}