package cn.kmbeast.pojo.dto.query.extend;

import cn.kmbeast.pojo.dto.query.PageQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 秒杀订单查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FlashSaleOrderQueryDto extends PageQueryDto {
    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 商品ID
     */
    private Integer itemId;

    /**
     * 订单状态
     */
    private Integer status;

    /**
     * 下单开始时间
     */
    private LocalDateTime orderTimeBegin;

    /**
     * 下单结束时间
     */
    private LocalDateTime orderTimeEnd;
}