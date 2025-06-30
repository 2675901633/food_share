package cn.kmbeast.service;

import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.extend.FlashSaleItemQueryDto;
import cn.kmbeast.pojo.dto.query.extend.FlashSaleOrderQueryDto;
import cn.kmbeast.pojo.entity.FlashSaleItem;
import cn.kmbeast.pojo.entity.FlashSaleOrder;
import cn.kmbeast.pojo.vo.FlashSaleItemVO;
import cn.kmbeast.pojo.vo.PageResult;

import java.util.List;

/**
 * 秒杀服务接口
 */
public interface FlashSaleService {

    /**
     * 查询秒杀商品列表
     * 
     * @param queryDto 查询条件
     * @return 秒杀商品列表
     */
    Result<List<FlashSaleItemVO>> queryFlashSaleItems(FlashSaleItemQueryDto queryDto);

    /**
     * 获取秒杀商品详情
     * 
     * @param itemId 商品ID
     * @return 商品详情
     */
    Result<FlashSaleItemVO> getFlashSaleItem(Integer itemId);

    /**
     * 创建秒杀商品
     * 
     * @param flashSaleItem 商品信息
     * @return 操作结果
     */
    Result<Void> createFlashSaleItem(FlashSaleItem flashSaleItem);

    /**
     * 更新秒杀商品
     * 
     * @param flashSaleItem 商品信息
     * @return 操作结果
     */
    Result<Void> updateFlashSaleItem(FlashSaleItem flashSaleItem);

    /**
     * 删除秒杀商品
     * 
     * @param itemId 商品ID
     * @return 操作结果
     */
    Result<Void> deleteFlashSaleItem(Integer itemId);

    /**
     * 秒杀下单
     * 
     * @param itemId 商品ID
     * @param userId 用户ID
     * @return 订单信息
     */
    Result<FlashSaleOrder> flashSale(Integer itemId, Integer userId);

    /**
     * 查询用户秒杀订单
     * 
     * @param userId 用户ID
     * @return 订单列表
     */
    Result<List<FlashSaleOrder>> queryUserOrders(Integer userId);

    /**
     * 查询所有秒杀订单（管理员使用）
     * 
     * @param queryDto 查询条件
     * @return 分页订单结果
     */
    Result<PageResult<FlashSaleOrder>> queryAllOrders(FlashSaleOrderQueryDto queryDto);

    /**
     * 取消订单
     * 
     * @param orderId 订单ID
     * @param userId  用户ID
     * @return 操作结果
     */
    Result<Void> cancelOrder(String orderId, Integer userId);

    /**
     * 支付订单
     * 
     * @param orderId 订单ID
     * @param userId  用户ID
     * @return 操作结果
     */
    Result<Void> payOrder(String orderId, Integer userId);

    /**
     * 秒杀库存预热
     * 
     * @return 操作结果
     */
    Result<Void> preloadFlashSaleStock();

    /**
     * 刷新秒杀商品状态
     * 
     * @return 操作结果
     */
    Result<Void> refreshFlashSaleStatus();

    /**
     * 手动结束秒杀活动
     * 
     * @param itemId 商品ID
     * @return 操作结果
     */
    Result<Void> endFlashSale(Integer itemId);

    /**
     * 强制结束秒杀活动（即使活动已开始）
     * 
     * @param itemId 商品ID
     * @return 操作结果
     */
    Result<Void> forceEndFlashSale(Integer itemId);
}