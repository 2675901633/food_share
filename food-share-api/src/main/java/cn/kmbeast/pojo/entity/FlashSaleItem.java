package cn.kmbeast.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀商品实体类
 */
@Data
@TableName("flash_sale_item")
public class FlashSaleItem {

    @TableId(value = "id", type = IdType.AUTO)
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

    private LocalDateTime updateTime;
}