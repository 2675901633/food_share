package cn.kmbeast.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀商品视图对象
 */
@Data
public class FlashSaleItemVO {

    private Integer id;

    private String name;

    private String description;

    private String image;

    private BigDecimal originalPrice;

    private BigDecimal flashPrice;

    private Integer stock;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer status; // 0-未开始，1-进行中，2-已结束

    private LocalDateTime createTime;

    // 附加字段
    private Long remainSeconds; // 距离开始或结束的剩余秒数

    private Boolean canBuy; // 当前用户是否可购买

    private Integer soldCount; // 已售数量
}