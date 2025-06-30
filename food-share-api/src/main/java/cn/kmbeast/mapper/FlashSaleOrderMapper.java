package cn.kmbeast.mapper;

import cn.kmbeast.pojo.dto.query.extend.FlashSaleOrderQueryDto;
import cn.kmbeast.pojo.entity.FlashSaleOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 秒杀订单Mapper接口
 */
@Mapper
public interface FlashSaleOrderMapper extends BaseMapper<FlashSaleOrder> {

    /**
     * 查询用户的秒杀订单
     * 
     * @param userId 用户ID
     * @return 秒杀订单列表
     */
    List<FlashSaleOrder> queryOrdersByUserId(@Param("userId") Integer userId);

    /**
     * 查询商品的秒杀订单
     * 
     * @param itemId 商品ID
     * @return 秒杀订单列表
     */
    List<FlashSaleOrder> queryOrdersByItemId(@Param("itemId") Integer itemId);

    /**
     * 查询用户是否已购买某商品
     * 
     * @param userId 用户ID
     * @param itemId 商品ID
     * @return 订单数量
     */
    int countUserOrder(@Param("userId") Integer userId, @Param("itemId") Integer itemId);

    /**
     * 查询所有秒杀订单（管理员使用）
     * 
     * @param queryDto 查询条件
     * @return 秒杀订单列表
     */
    List<FlashSaleOrder> queryAllOrders(@Param("queryDto") FlashSaleOrderQueryDto queryDto);

    /**
     * 统计商品的秒杀订单数量（已售数量）
     * 
     * @param itemId 商品ID
     * @return 订单数量
     */
    int countOrdersByItemId(@Param("itemId") Integer itemId);
}