package cn.kmbeast.controller;

import cn.kmbeast.context.LocalThreadHolder;
import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.extend.FlashSaleItemQueryDto;
import cn.kmbeast.pojo.dto.query.extend.FlashSaleOrderQueryDto;
import cn.kmbeast.pojo.entity.FlashSaleItem;
import cn.kmbeast.pojo.entity.FlashSaleOrder;
import cn.kmbeast.pojo.vo.FlashSaleItemVO;
import cn.kmbeast.pojo.vo.PageResult;
import cn.kmbeast.service.FlashSaleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 秒杀控制器
 */
@Slf4j
@RestController
@RequestMapping("/flashSale")
public class FlashSaleController {

    @Resource
    private FlashSaleService flashSaleService;

    /**
     * 查询秒杀商品列表
     *
     * @param queryDto 查询条件
     * @return 商品列表
     */
    @PostMapping("/list")
    public Result<List<FlashSaleItemVO>> listFlashSaleItems(@RequestBody FlashSaleItemQueryDto queryDto) {
        return flashSaleService.queryFlashSaleItems(queryDto);
    }

    /**
     * 获取秒杀商品详情
     *
     * @param itemId 商品ID
     * @return 商品详情
     */
    @GetMapping("/detail/{itemId}")
    public Result<FlashSaleItemVO> getFlashSaleItemDetail(@PathVariable Integer itemId) {
        return flashSaleService.getFlashSaleItem(itemId);
    }

    /**
     * 执行秒杀
     *
     * @param itemId 商品ID
     * @return 订单信息
     */
    @PostMapping("/doFlashSale/{itemId}")
    public Result<FlashSaleOrder> doFlashSale(@PathVariable Integer itemId) {
        Integer userId = LocalThreadHolder.getUserId();
        return flashSaleService.flashSale(itemId, userId);
    }

    /**
     * 查询用户订单
     *
     * @return 订单列表
     */
    @GetMapping("/myOrders")
    public Result<List<FlashSaleOrder>> queryMyOrders() {
        Integer userId = LocalThreadHolder.getUserId();
        return flashSaleService.queryUserOrders(userId);
    }

    /**
     * 管理员接口 - 查询所有订单
     *
     * @param queryDto 查询条件
     * @return 订单列表
     */
    @PostMapping("/orders")
    public Result<PageResult<FlashSaleOrder>> queryAllOrders(@RequestBody FlashSaleOrderQueryDto queryDto) {
        return flashSaleService.queryAllOrders(queryDto);
    }

    /**
     * 取消订单
     *
     * @param orderId 订单ID
     * @return 操作结果
     */
    @PostMapping("/cancelOrder/{orderId}")
    public Result<Void> cancelOrder(@PathVariable String orderId) {
        Integer userId = LocalThreadHolder.getUserId();
        return flashSaleService.cancelOrder(orderId, userId);
    }

    /**
     * 支付订单
     *
     * @param orderId 订单ID
     * @return 操作结果
     */
    @PostMapping("/payOrder/{orderId}")
    public Result<Void> payOrder(@PathVariable String orderId) {
        Integer userId = LocalThreadHolder.getUserId();
        return flashSaleService.payOrder(orderId, userId);
    }

    /**
     * 管理员接口 - 创建秒杀商品
     *
     * @param flashSaleItem 商品信息
     * @return 操作结果
     */
    @PostMapping("/admin/create")
    public Result<Void> createFlashSaleItem(@RequestBody FlashSaleItem flashSaleItem) {
        return flashSaleService.createFlashSaleItem(flashSaleItem);
    }

    /**
     * 管理员接口 - 更新秒杀商品
     *
     * @param flashSaleItem 商品信息
     * @return 操作结果
     */
    @PostMapping("/admin/update")
    public Result<Void> updateFlashSaleItem(@RequestBody FlashSaleItem flashSaleItem) {
        return flashSaleService.updateFlashSaleItem(flashSaleItem);
    }

    /**
     * 管理员接口 - 删除秒杀商品
     *
     * @param itemId 商品ID
     * @return 操作结果
     */
    @PostMapping("/admin/delete/{itemId}")
    public Result<Void> deleteFlashSaleItem(@PathVariable Integer itemId) {
        return flashSaleService.deleteFlashSaleItem(itemId);
    }

    /**
     * 管理员接口 - 手动预热秒杀库存
     *
     * @return 操作结果
     */
    @PostMapping("/admin/preload")
    public Result<Void> preloadFlashSaleStock() {
        return flashSaleService.preloadFlashSaleStock();
    }

    /**
     * 管理员接口 - 刷新秒杀商品状态
     *
     * @return 操作结果
     */
    @PostMapping("/admin/refresh")
    public Result<Void> refreshFlashSaleStatus() {
        return flashSaleService.refreshFlashSaleStatus();
    }

    /**
     * 管理员接口 - 手动结束秒杀活动
     *
     * @param itemId 商品ID
     * @return 操作结果
     */
    @PostMapping("/admin/endSale/{itemId}")
    public Result<Void> endFlashSale(@PathVariable Integer itemId) {
        return flashSaleService.endFlashSale(itemId);
    }

    /**
     * 管理员接口 - 强制结束秒杀活动
     *
     * @param item 包含ID和状态的商品信息
     * @return 操作结果
     */
    @PostMapping("/admin/forceEndSale")
    public Result<Void> forceEndFlashSale(@RequestBody FlashSaleItem item) {
        if (item == null || item.getId() == null) {
            return ApiResult.error("参数不完整");
        }
        return flashSaleService.forceEndFlashSale(item.getId());
    }
}