package cn.kmbeast.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀订单实体类
 */
@Data
@TableName("flash_sale_order")
public class FlashSaleOrder {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String orderId;

    private Integer userId;

    private Integer itemId;

    private BigDecimal price;

    private LocalDateTime orderTime;

    private Integer status; // 1-已下单，2-已支付，3-已取消

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}