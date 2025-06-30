package cn.kmbeast.pojo.dto.query.extend;

import cn.kmbeast.pojo.dto.query.PageQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 秒杀商品查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FlashSaleItemQueryDto extends PageQueryDto {

    /**
     * 商品ID
     */
    private Integer id;

    private String name;

    private Integer status;

    private LocalDateTime startTimeBegin;

    private LocalDateTime startTimeEnd;

    private LocalDateTime endTimeBegin;

    private LocalDateTime endTimeEnd;

    /**
     * 是否为管理员请求，true表示跳过缓存直接查询数据库
     */
    private Boolean admin;
}