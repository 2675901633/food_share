package cn.kmbeast.service.impl;

import cn.kmbeast.context.LocalThreadHolder;
import cn.kmbeast.event.NotificationMessage;
import cn.kmbeast.mapper.FlashSaleItemMapper;
import cn.kmbeast.mapper.FlashSaleOrderMapper;
import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.extend.FlashSaleItemQueryDto;
import cn.kmbeast.pojo.dto.query.extend.FlashSaleOrderQueryDto;
import cn.kmbeast.pojo.entity.FlashSaleItem;
import cn.kmbeast.pojo.entity.FlashSaleOrder;
import cn.kmbeast.pojo.vo.FlashSaleItemVO;
import cn.kmbeast.pojo.vo.PageResult;
import cn.kmbeast.service.FlashSaleService;
import cn.kmbeast.service.NotificationService;
import cn.kmbeast.utils.CacheConstants;
import cn.kmbeast.utils.DateUtil;
import cn.kmbeast.utils.IdFactoryUtil;
import cn.kmbeast.utils.RedisUtil;
import com.alibaba.fastjson2.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 秒杀服务实现
 */
@Slf4j
@Service
public class FlashSaleServiceImpl implements FlashSaleService {

    @Resource
    private FlashSaleItemMapper flashSaleItemMapper;

    @Resource
    private FlashSaleOrderMapper flashSaleOrderMapper;

    @Resource
    private NotificationService notificationService;

    @Resource
    private RedisUtil redisUtil;

    @Override
    public Result<List<FlashSaleItemVO>> queryFlashSaleItems(FlashSaleItemQueryDto queryDto) {
        try {
            // 检查请求参数中是否有特定标识（如admin=true）表示需要实时数据
            boolean isAdminRequest = queryDto != null && Boolean.TRUE.equals(queryDto.getAdmin());

            // 如果是管理员请求或有特定ID查询，直接从数据库查询
            if (isAdminRequest || (queryDto != null && queryDto.getId() != null)) {
                log.debug("管理员查询或ID查询，跳过缓存，直接查询数据库");
                List<FlashSaleItemVO> itemList = flashSaleItemMapper.queryFlashSaleItems(queryDto);
                if (!CollectionUtils.isEmpty(itemList)) {
                    // 处理额外信息
                    processFlashSaleItems(itemList);

                    // 对于管理员请求，更新缓存但优先返回最新数据
                    if (isAdminRequest) {
                        // 异步更新缓存
                        try {
                            redisUtil.set(CacheConstants.FLASH_SALE_ITEM_LIST, itemList,
                                    CacheConstants.FLASH_SALE_ITEM_EXPIRE);
                        } catch (Exception e) {
                            log.warn("管理员请求更新缓存失败，不影响返回结果：{}", e.getMessage());
                        }
                    }

                    return ApiResult.success(itemList);
                }
                return ApiResult.success(new ArrayList<>());
            }

            // 普通请求尝试从缓存获取秒杀商品列表
            Object cacheData = redisUtil.get(CacheConstants.FLASH_SALE_ITEM_LIST);

            if (cacheData != null) {
                @SuppressWarnings("unchecked")
                List<FlashSaleItemVO> cachedList = (List<FlashSaleItemVO>) cacheData;
                return ApiResult.success(filterFlashSaleItems(cachedList, queryDto));
            }

            // 缓存不存在，从数据库查询
            List<FlashSaleItemVO> itemList = flashSaleItemMapper.queryFlashSaleItems(queryDto);
            if (!CollectionUtils.isEmpty(itemList)) {
                // 处理额外信息
                processFlashSaleItems(itemList);
                // 更新缓存
                redisUtil.set(CacheConstants.FLASH_SALE_ITEM_LIST, itemList, CacheConstants.FLASH_SALE_ITEM_EXPIRE);
                return ApiResult.success(itemList);
            }

            return ApiResult.success(new ArrayList<>());
        } catch (Exception e) {
            log.error("查询秒杀商品列表失败: {}", e.getMessage(), e);
            return ApiResult.error("查询秒杀商品列表失败");
        }
    }

    @Override
    public Result<FlashSaleItemVO> getFlashSaleItem(Integer itemId) {
        if (itemId == null) {
            return ApiResult.error("商品ID不能为空");
        }

        try {
            // 从缓存获取商品详情
            String cacheKey = CacheConstants.FLASH_SALE_ITEM_PREFIX + itemId;
            Object cacheData = redisUtil.get(cacheKey);

            if (cacheData != null) {
                FlashSaleItemVO itemVO = (FlashSaleItemVO) cacheData;
                // 刷新剩余时间等动态信息
                processFlashSaleItem(itemVO);
                return ApiResult.success(itemVO);
            }

            // 缓存不存在，从数据库查询
            FlashSaleItemVO itemVO = flashSaleItemMapper.getFlashSaleItemById(itemId);
            if (itemVO != null) {
                // 处理额外信息
                processFlashSaleItem(itemVO);
                // 更新缓存
                redisUtil.set(cacheKey, itemVO, CacheConstants.FLASH_SALE_ITEM_EXPIRE);
                return ApiResult.success(itemVO);
            }

            return ApiResult.error("秒杀商品不存在");
        } catch (Exception e) {
            log.error("获取秒杀商品详情失败: {}", e.getMessage(), e);
            return ApiResult.error("获取秒杀商品详情失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> createFlashSaleItem(FlashSaleItem flashSaleItem) {
        try {
            // 参数校验
            if (flashSaleItem == null) {
                return ApiResult.error("商品信息不能为空");
            }

            if (!StringUtils.hasText(flashSaleItem.getName()) || flashSaleItem.getStock() == null
                    || flashSaleItem.getFlashPrice() == null || flashSaleItem.getStartTime() == null
                    || flashSaleItem.getEndTime() == null) {
                return ApiResult.error("商品信息不完整");
            }

            if (flashSaleItem.getStock() <= 0) {
                return ApiResult.error("库存必须大于0");
            }

            if (flashSaleItem.getStartTime().isAfter(flashSaleItem.getEndTime())) {
                return ApiResult.error("结束时间必须晚于开始时间");
            }

            // 设置默认值
            flashSaleItem.setStatus(0); // 初始状态为未开始
            flashSaleItem.setCreateTime(LocalDateTime.now());
            flashSaleItem.setUpdateTime(LocalDateTime.now());

            // 保存到数据库
            flashSaleItemMapper.insert(flashSaleItem);

            log.info("新创建秒杀商品: ID={}, 名称={}", flashSaleItem.getId(), flashSaleItem.getName());

            // 清除缓存，确保列表数据实时更新
            redisUtil.del(CacheConstants.FLASH_SALE_ITEM_LIST);

            // 强制刷新缓存数据，确保新商品立即可见
            try {
                // 按商品ID再次从数据库查询确保拿到最新数据
                FlashSaleItemVO newItem = flashSaleItemMapper.getFlashSaleItemById(flashSaleItem.getId());
                if (newItem != null) {
                    // 处理商品信息
                    processFlashSaleItem(newItem);
                    // 更新商品缓存
                    redisUtil.set(
                            CacheConstants.FLASH_SALE_ITEM_PREFIX + newItem.getId(),
                            newItem,
                            CacheConstants.FLASH_SALE_ITEM_EXPIRE);
                }
            } catch (Exception cacheEx) {
                log.warn("更新商品缓存失败，但不影响创建过程: {}", cacheEx.getMessage());
            }

            // 如果商品即将开始或已开始，预热库存
            if (flashSaleItem.getStartTime().isBefore(LocalDateTime.now().plusHours(1))) {
                preloadItemStock(flashSaleItem.getId(), flashSaleItem.getStock());
            }

            // 发送秒杀商品发布通知给所有用户
            try {
                sendFlashSalePublishNotification(flashSaleItem);
            } catch (Exception notifyEx) {
                log.warn("发送秒杀通知失败，但不影响创建过程: {}", notifyEx.getMessage());
            }

            return ApiResult.success();
        } catch (Exception e) {
            log.error("创建秒杀商品失败: {}", e.getMessage(), e);
            return ApiResult.error("创建秒杀商品失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateFlashSaleItem(FlashSaleItem flashSaleItem) {
        try {
            // 参数校验
            if (flashSaleItem == null || flashSaleItem.getId() == null) {
                return ApiResult.error("商品信息不完整");
            }

            // 获取数据库中的商品信息
            FlashSaleItem existingItem = flashSaleItemMapper.selectById(flashSaleItem.getId());
            if (existingItem == null) {
                return ApiResult.error("商品不存在");
            }

            // 已开始的活动不能修改基本信息，但可以结束活动
            if (existingItem.getStatus() == 1 && flashSaleItem.getStatus() != 2) {
                return ApiResult.error("秒杀活动已开始，无法修改基本信息");
            }

            // 更新时间
            flashSaleItem.setUpdateTime(LocalDateTime.now());

            // 保存到数据库
            flashSaleItemMapper.updateById(flashSaleItem);

            // 清除缓存
            redisUtil.del(CacheConstants.FLASH_SALE_ITEM_LIST);
            redisUtil.del(CacheConstants.FLASH_SALE_ITEM_PREFIX + flashSaleItem.getId());

            // 如果商品即将开始或已开始，刷新库存缓存
            if (flashSaleItem.getStartTime() != null
                    && flashSaleItem.getStartTime().isBefore(LocalDateTime.now().plusHours(1))) {
                if (flashSaleItem.getStock() != null && flashSaleItem.getStock() > 0) {
                    preloadItemStock(flashSaleItem.getId(), flashSaleItem.getStock());
                }
            }

            return ApiResult.success();
        } catch (Exception e) {
            log.error("更新秒杀商品失败: {}", e.getMessage(), e);
            return ApiResult.error("更新秒杀商品失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteFlashSaleItem(Integer itemId) {
        try {
            // 参数校验
            if (itemId == null) {
                return ApiResult.error("商品ID不能为空");
            }

            // 获取数据库中的商品信息
            FlashSaleItem existingItem = flashSaleItemMapper.selectById(itemId);
            if (existingItem == null) {
                return ApiResult.error("商品不存在");
            }

            // 已开始的活动不能删除
            if (existingItem.getStatus() == 1) {
                return ApiResult.error("秒杀活动已开始，无法删除");
            }

            // 删除商品
            flashSaleItemMapper.deleteById(itemId);

            // 清除缓存
            redisUtil.del(CacheConstants.FLASH_SALE_ITEM_LIST);
            redisUtil.del(CacheConstants.FLASH_SALE_ITEM_PREFIX + itemId);
            redisUtil.del(CacheConstants.FLASH_SALE_STOCK_PREFIX + itemId);

            return ApiResult.success();
        } catch (Exception e) {
            log.error("删除秒杀商品失败: {}", e.getMessage(), e);
            return ApiResult.error("删除秒杀商品失败");
        }
    }

    @Override
    public Result<FlashSaleOrder> flashSale(Integer itemId, Integer userId) {
        if (itemId == null) {
            return ApiResult.error("商品ID不能为空");
        }

        if (userId == null) {
            userId = LocalThreadHolder.getUserId();
            if (userId == null) {
                return ApiResult.error("用户未登录");
            }
        }

        // 接口限流 - 使用INCR+EXPIRE组合实现
        String rateLimitKey = CacheConstants.FLASH_SALE_RATE_LIMIT_PREFIX + userId + ":" + itemId;
        Long accessCount = redisUtil.incr(rateLimitKey, 1);
        if (accessCount == 1) {
            // 设置过期时间，实现滑动窗口
            redisUtil.expire(rateLimitKey, CacheConstants.FLASH_SALE_RATE_LIMIT_PERIOD);
        }

        if (accessCount > CacheConstants.FLASH_SALE_RATE_LIMIT_COUNT) {
            return ApiResult.error("请求过于频繁，请稍后再试");
        }

        try {
            // 1. 检查秒杀记录，防止重复下单
            String recordKey = CacheConstants.FLASH_SALE_USER_RECORD_PREFIX + userId + ":" + itemId;
            if (Boolean.TRUE.equals(redisUtil.hasKey(recordKey))) {
                return ApiResult.error("您已参与过此秒杀，请勿重复下单");
            }

            // 2. 从缓存获取秒杀商品信息
            String itemKey = CacheConstants.FLASH_SALE_ITEM_PREFIX + itemId;
            Object itemObj = redisUtil.get(itemKey);
            FlashSaleItemVO itemVO;

            if (itemObj == null) {
                // 缓存未命中，从数据库获取
                itemVO = flashSaleItemMapper.getFlashSaleItemById(itemId);
                if (itemVO == null) {
                    return ApiResult.error("秒杀商品不存在");
                }
                processFlashSaleItem(itemVO);
            } else {
                itemVO = (FlashSaleItemVO) itemObj;
                processFlashSaleItem(itemVO);
            }

            // 3. 校验秒杀活动状态
            if (itemVO.getStatus() != 1) {
                return ApiResult.error("秒杀活动未开始或已结束");
            }

            // 4. 使用Redis SETNX实现分布式锁，防止并发问题
            String lockKey = CacheConstants.FLASH_SALE_LOCK_PREFIX + itemId;
            boolean acquiredLock = redisUtil.set(lockKey, userId, CacheConstants.FLASH_SALE_LOCK_EXPIRE);

            if (!acquiredLock) {
                return ApiResult.error("系统繁忙，请稍后再试");
            }

            try {
                // 5. 检查库存 - 确保Redis中存在库存记录
                String stockKey = CacheConstants.FLASH_SALE_STOCK_PREFIX + itemId;
                Object stockObj = redisUtil.get(stockKey);

                // 如果Redis中没有库存数据，先预热库存
                if (stockObj == null) {
                    log.warn("商品 {} 缺少Redis库存数据，执行库存预热", itemId);
                    FlashSaleItem item = flashSaleItemMapper.selectById(itemId);
                    if (item != null && item.getStock() > 0) {
                        preloadItemStock(itemId, item.getStock());
                    } else {
                        return ApiResult.error("商品库存数据异常");
                    }
                }

                // 使用Redis的DECR原子操作扣减库存
                long remainingStock = redisUtil.decr(stockKey, 1);

                if (remainingStock < 0) {
                    // 库存不足，恢复库存
                    redisUtil.incr(stockKey, 1);
                    return ApiResult.error("秒杀商品已售罄");
                }

                // 预占库存成功，检查数据库库存状态
                FlashSaleItem dbItem = flashSaleItemMapper.selectById(itemId);
                if (dbItem != null && dbItem.getStock() > 0) {
                    // 预占库存，但数据库库存暂不更新，在支付完成后再实际扣减
                    if (remainingStock == 0) {
                        log.info("商品 {} 预占库存已满，暂时标记为售罄状态", itemId);

                        // 标记商品为售罄状态，但不实际减库存
                        // 创建临时售罄标记，用于前端显示
                        String tempSoldOutKey = CacheConstants.FLASH_SALE_ITEM_PREFIX + itemId + ":tempSoldOut";
                        redisUtil.set(tempSoldOutKey, true, CacheConstants.FLASH_SALE_ITEM_INFO_EXPIRE);
                    }
                }

                // 创建订单
                FlashSaleOrder order = createOrder(userId, itemVO);

                // 记录用户秒杀信息，防止重复下单
                redisUtil.set(recordKey, order.getOrderId(), CacheConstants.FLASH_SALE_USER_RECORD_EXPIRE);

                // 记录订单对应的预占库存，用于支付或取消时使用
                String orderStockKey = CacheConstants.FLASH_SALE_ITEM_PREFIX + "order:" + order.getOrderId();
                redisUtil.set(orderStockKey, itemId, CacheConstants.FLASH_SALE_ORDER_EXPIRE);

                return ApiResult.success(order);
            } finally {
                // 释放锁
                redisUtil.del(lockKey);
            }
        } catch (Exception e) {
            log.error("秒杀下单失败: {}", e.getMessage(), e);
            return ApiResult.error("秒杀下单失败");
        }
    }

    @Override
    public Result<List<FlashSaleOrder>> queryUserOrders(Integer userId) {
        if (userId == null) {
            userId = LocalThreadHolder.getUserId();
            if (userId == null) {
                return ApiResult.error("用户未登录");
            }
        }

        try {
            List<FlashSaleOrder> orders = flashSaleOrderMapper.queryOrdersByUserId(userId);
            return ApiResult.success(orders);
        } catch (Exception e) {
            log.error("查询用户秒杀订单失败: {}", e.getMessage(), e);
            return ApiResult.error("查询用户秒杀订单失败");
        }
    }

    @Override
    public Result<PageResult<FlashSaleOrder>> queryAllOrders(FlashSaleOrderQueryDto queryDto) {
        if (queryDto == null) {
            queryDto = new FlashSaleOrderQueryDto();
        }

        try {
            // 使用PageHelper进行分页
            PageHelper.startPage(queryDto.getCurrent(), queryDto.getSize());

            // 查询订单
            List<FlashSaleOrder> orders = flashSaleOrderMapper.queryAllOrders(queryDto);

            // 获取分页信息
            Page<FlashSaleOrder> page = (Page<FlashSaleOrder>) orders;

            // 构造分页结果
            PageResult<FlashSaleOrder> pageResult = PageResult.of(
                    orders,
                    page.getTotal(),
                    queryDto.getCurrent(),
                    queryDto.getSize());

            return ApiResult.success(pageResult);
        } catch (Exception e) {
            log.error("查询所有秒杀订单失败: {}", e.getMessage(), e);
            return ApiResult.error("查询所有秒杀订单失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> cancelOrder(String orderId, Integer userId) {
        if (!StringUtils.hasText(orderId)) {
            return ApiResult.error("订单ID不能为空");
        }

        if (userId == null) {
            userId = LocalThreadHolder.getUserId();
            if (userId == null) {
                return ApiResult.error("用户未登录");
            }
        }

        try {
            // 查询订单
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<FlashSaleOrder> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            queryWrapper.eq("order_id", orderId);
            FlashSaleOrder order = flashSaleOrderMapper.selectOne(queryWrapper);

            if (order == null) {
                return ApiResult.error("订单不存在");
            }

            // 验证用户身份
            if (!order.getUserId().equals(userId)) {
                return ApiResult.error("无权操作此订单");
            }

            // 检查订单状态
            if (order.getStatus() != 1) {
                return ApiResult.error("订单状态不允许取消");
            }

            // 获取商品ID
            Integer itemId = order.getItemId();

            // 分布式锁
            String lockKey = CacheConstants.FLASH_SALE_LOCK_PREFIX + "cancel:" + itemId;
            boolean acquiredLock = redisUtil.set(lockKey, userId, CacheConstants.FLASH_SALE_LOCK_EXPIRE);

            if (!acquiredLock) {
                return ApiResult.error("系统繁忙，请稍后再试");
            }

            try {
                // 取消订单
                order.setStatus(3); // 设置为已取消
                order.setUpdateTime(LocalDateTime.now());
                flashSaleOrderMapper.updateById(order);

                // 恢复Redis库存
                String stockKey = CacheConstants.FLASH_SALE_STOCK_PREFIX + order.getItemId();
                redisUtil.incr(stockKey, 1);

                // 清除临时售罄标记（如果有）
                String tempSoldOutKey = CacheConstants.FLASH_SALE_ITEM_PREFIX + itemId + ":tempSoldOut";
                redisUtil.del(tempSoldOutKey);

                // 清除商品缓存，确保获取最新状态
                redisUtil.del(CacheConstants.FLASH_SALE_ITEM_PREFIX + itemId);

                // 清除订单相关的预占记录
                String orderStockKey = CacheConstants.FLASH_SALE_ITEM_PREFIX + "order:" + order.getOrderId();
                redisUtil.del(orderStockKey);

                log.info("订单 {} 取消成功，释放商品 {} 的预占库存", orderId, itemId);

                // 删除用户秒杀记录
                String recordKey = CacheConstants.FLASH_SALE_USER_RECORD_PREFIX + userId + ":" + order.getItemId();
                redisUtil.del(recordKey);

                return ApiResult.success();
            } finally {
                // 释放锁
                redisUtil.del(lockKey);
            }
        } catch (Exception e) {
            log.error("取消订单失败: {}", e.getMessage(), e);
            return ApiResult.error("取消订单失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> payOrder(String orderId, Integer userId) {
        if (!StringUtils.hasText(orderId)) {
            return ApiResult.error("订单ID不能为空");
        }

        if (userId == null) {
            userId = LocalThreadHolder.getUserId();
            if (userId == null) {
                return ApiResult.error("用户未登录");
            }
        }

        try {
            // 查询订单
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<FlashSaleOrder> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            queryWrapper.eq("order_id", orderId);
            FlashSaleOrder order = flashSaleOrderMapper.selectOne(queryWrapper);

            if (order == null) {
                return ApiResult.error("订单不存在");
            }

            // 验证用户身份
            if (!order.getUserId().equals(userId)) {
                return ApiResult.error("无权操作此订单");
            }

            // 检查订单状态
            if (order.getStatus() != 1) {
                return ApiResult.error("订单状态不允许支付");
            }

            // 获取商品ID
            Integer itemId = order.getItemId();

            // 使用Redis分布式锁防止并发操作
            String lockKey = CacheConstants.FLASH_SALE_LOCK_PREFIX + "pay:" + itemId;
            boolean acquiredLock = redisUtil.set(lockKey, userId, CacheConstants.FLASH_SALE_LOCK_EXPIRE);

            if (!acquiredLock) {
                return ApiResult.error("系统繁忙，请稍后再试");
            }

            try {
                // 验证商品是否仍在活动期间内
                FlashSaleItem item = flashSaleItemMapper.selectById(itemId);
                if (item == null) {
                    return ApiResult.error("商品不存在，无法支付");
                }

                if (item.getStatus() != 1) {
                    // 如果活动已结束，允许支付但提示用户
                    log.warn("订单 {} 对应商品活动已结束，但仍允许支付", orderId);
                }

                // 检查商品是否已售罄（其他用户已经支付导致库存为0）
                if (item.getStock() <= 0) {
                    // 如果库存已为0，但当前用户已经预占了库存，仍然允许支付
                    String orderStockKey = CacheConstants.FLASH_SALE_ITEM_PREFIX + "order:" + order.getOrderId();
                    if (!Boolean.TRUE.equals(redisUtil.hasKey(orderStockKey))) {
                        // 如果没有预占记录，说明库存已被释放
                        return ApiResult.error("秒杀商品已售罄，无法支付");
                    }
                }

                // 更新订单状态
                order.setStatus(2); // 设置为已支付
                order.setUpdateTime(LocalDateTime.now());
                flashSaleOrderMapper.updateById(order);

                // 实际扣减数据库库存
                flashSaleItemMapper.decreaseStock(itemId);

                // 获取当前商品信息，检查是否售罄
                FlashSaleItem updatedItem = flashSaleItemMapper.selectById(itemId);
                if (updatedItem != null && updatedItem.getStock() <= 0) {
                    log.info("商品 {} 已售罄", itemId);

                    // 设置售罄标记
                    String soldOutKey = CacheConstants.FLASH_SALE_ITEM_PREFIX + itemId + ":soldOut";
                    redisUtil.set(soldOutKey, true, CacheConstants.FLASH_SALE_ITEM_INFO_EXPIRE);

                    // 库存为0时自动结束秒杀活动
                    if (updatedItem.getStatus() == 1) {
                        log.info("商品 {} 库存为0，自动将状态修改为已结束", itemId);
                        updatedItem.setStatus(2); // 设置为已结束状态
                        updatedItem.setUpdateTime(LocalDateTime.now());
                        flashSaleItemMapper.updateById(updatedItem);

                        // 清除缓存，确保状态更新立即生效
                        redisUtil.del(CacheConstants.FLASH_SALE_ITEM_LIST);
                        redisUtil.del(CacheConstants.FLASH_SALE_ITEM_PREFIX + itemId);
                    }
                }

                // 更新销量缓存
                String soldCountKey = CacheConstants.FLASH_SALE_ITEM_PREFIX + itemId + ":soldCount";
                Object soldCountObj = redisUtil.get(soldCountKey);
                int currentSoldCount = soldCountObj != null ? Integer.parseInt(soldCountObj.toString()) : 0;
                redisUtil.set(soldCountKey, currentSoldCount + 1, CacheConstants.FLASH_SALE_ITEM_INFO_EXPIRE);

                // 清除临时数据
                String orderStockKey = CacheConstants.FLASH_SALE_ITEM_PREFIX + "order:" + order.getOrderId();
                redisUtil.del(orderStockKey);

                // 清除订单相关商品缓存，确保查询到的是最新状态
                redisUtil.del(CacheConstants.FLASH_SALE_ITEM_PREFIX + itemId);

                log.info("订单 {} 支付成功，商品 {}", orderId, itemId);

                return ApiResult.success();
            } finally {
                // 释放锁
                redisUtil.del(lockKey);
            }
        } catch (Exception e) {
            log.error("支付订单失败: {}", e.getMessage(), e);
            return ApiResult.error("支付订单失败");
        }
    }

    @Override
    public Result<Void> preloadFlashSaleStock() {
        try {
            // 查询所有秒杀商品ID
            List<Integer> itemIds = flashSaleItemMapper.getAllItemIds();
            if (CollectionUtils.isEmpty(itemIds)) {
                return ApiResult.success();
            }

            int count = 0;
            for (Integer itemId : itemIds) {
                FlashSaleItem item = flashSaleItemMapper.selectById(itemId);
                if (item != null && item.getStartTime().isBefore(LocalDateTime.now().plusHours(1))) {
                    preloadItemStock(itemId, item.getStock());
                    count++;
                }
            }

            log.info("预热秒杀库存完成，共预热 {} 个商品", count);
            return ApiResult.success();
        } catch (Exception e) {
            log.error("预热秒杀库存失败: {}", e.getMessage(), e);
            return ApiResult.error("预热秒杀库存失败");
        }
    }

    @Override
    public Result<Void> refreshFlashSaleStatus() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<FlashSaleItem> items = flashSaleItemMapper.selectList(null);

            for (FlashSaleItem item : items) {
                // 更新商品状态
                int oldStatus = item.getStatus();
                int newStatus;

                if (now.isBefore(item.getStartTime())) {
                    newStatus = 0; // 未开始
                } else if (now.isAfter(item.getEndTime())) {
                    newStatus = 2; // 已结束
                } else {
                    newStatus = 1; // 进行中
                }

                // 状态变化时更新数据库和缓存
                if (oldStatus != newStatus) {
                    item.setStatus(newStatus);
                    item.setUpdateTime(now);
                    flashSaleItemMapper.updateById(item);

                    // 更新缓存
                    String itemKey = CacheConstants.FLASH_SALE_ITEM_PREFIX + item.getId();
                    redisUtil.del(itemKey);

                    // 对于新开始的活动，预热库存
                    if (newStatus == 1 && oldStatus == 0) {
                        preloadItemStock(item.getId(), item.getStock());
                    }
                }
            }

            // 清除商品列表缓存，强制刷新
            redisUtil.del(CacheConstants.FLASH_SALE_ITEM_LIST);

            return ApiResult.success();
        } catch (Exception e) {
            log.error("刷新秒杀商品状态失败: {}", e.getMessage(), e);
            return ApiResult.error("刷新秒杀商品状态失败");
        }
    }

    /**
     * 预热单个商品库存到Redis
     *
     * @param itemId 商品ID
     * @param stock  库存数量
     */
    private void preloadItemStock(Integer itemId, Integer stock) {
        // 确保库存不为负数
        int safeStock = Math.max(0, stock);

        // 检查Redis中是否已有库存数据
        String stockKey = CacheConstants.FLASH_SALE_STOCK_PREFIX + itemId;
        Object existingStock = redisUtil.get(stockKey);

        if (existingStock != null) {
            log.info("商品 {} 库存已存在于Redis中，当前值: {}", itemId, existingStock);
        } else {
            redisUtil.set(stockKey, safeStock, CacheConstants.FLASH_SALE_STOCK_EXPIRE);
            log.info("预热商品 {} 库存: {}", itemId, safeStock);
        }
    }

    /**
     * 创建秒杀订单
     *
     * @param userId 用户ID
     * @param item   秒杀商品信息
     * @return 订单信息
     */
    private FlashSaleOrder createOrder(Integer userId, FlashSaleItemVO item) {
        FlashSaleOrder order = new FlashSaleOrder();
        order.setOrderId(generateOrderId(userId, item.getId()));
        order.setUserId(userId);
        order.setItemId(item.getId());
        order.setPrice(item.getFlashPrice());
        order.setOrderTime(LocalDateTime.now());
        order.setStatus(1); // 已下单
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        // 保存订单
        flashSaleOrderMapper.insert(order);

        return order;
    }

    /**
     * 生成订单ID
     *
     * @param userId 用户ID
     * @param itemId 商品ID
     * @return 订单ID
     */
    private String generateOrderId(Integer userId, Integer itemId) {
        return "FLASH" + System.currentTimeMillis() + userId + itemId + new Random().nextInt(10000);
    }

    /**
     * 处理秒杀商品列表的额外信息
     *
     * @param items 商品列表
     */
    private void processFlashSaleItems(List<FlashSaleItemVO> items) {
        if (CollectionUtils.isEmpty(items)) {
            return;
        }

        for (FlashSaleItemVO item : items) {
            processFlashSaleItem(item);
        }
    }

    /**
     * 处理单个秒杀商品的额外信息
     *
     * @param item 商品信息
     */
    private void processFlashSaleItem(FlashSaleItemVO item) {
        if (item == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        Integer userId = LocalThreadHolder.getUserId();

        // 计算剩余秒数
        if (now.isBefore(item.getStartTime())) {
            // 未开始
            item.setRemainSeconds(Duration.between(now, item.getStartTime()).getSeconds());
        } else if (now.isAfter(item.getEndTime())) {
            // 已结束
            item.setRemainSeconds(0L);
        } else {
            // 进行中
            item.setRemainSeconds(Duration.between(now, item.getEndTime()).getSeconds());
        }

        // 检查用户是否可购买
        boolean canBuy = true;
        if (userId != null) {
            String recordKey = CacheConstants.FLASH_SALE_USER_RECORD_PREFIX + userId + ":" + item.getId();
            canBuy = !Boolean.TRUE.equals(redisUtil.hasKey(recordKey));
        }
        item.setCanBuy(canBuy);

        // 获取已售数量 - 优化计算方式，优先使用Redis库存计算，减少数据库访问
        try {
            // 首先检查是否存在售罄标记
            String soldOutKey = CacheConstants.FLASH_SALE_ITEM_PREFIX + item.getId() + ":soldOut";
            Boolean isSoldOut = (Boolean) redisUtil.get(soldOutKey);

            // 存在临时售罄标记，也认为可能售罄
            String tempSoldOutKey = CacheConstants.FLASH_SALE_ITEM_PREFIX + item.getId() + ":tempSoldOut";
            Boolean isTempSoldOut = (Boolean) redisUtil.get(tempSoldOutKey);

            // 如果已售罄，确保显示库存为0
            if (Boolean.TRUE.equals(isSoldOut) || Boolean.TRUE.equals(isTempSoldOut)) {
                if (item.getStock() > 0) {
                    log.debug("商品 {} 已标记为售罄，但库存仍显示为 {}，设置为0", item.getId(), item.getStock());
                    item.setStock(0);
                }
            }

            // 1. 先尝试从Redis缓存获取商品销量
            String soldCountKey = CacheConstants.FLASH_SALE_ITEM_PREFIX + item.getId() + ":soldCount";
            Object soldCountObj = redisUtil.get(soldCountKey);

            if (soldCountObj != null) {
                // 如果Redis中有销量数据，直接使用
                item.setSoldCount(Integer.parseInt(soldCountObj.toString()));
            } else {
                // 2. 如果Redis中没有销量数据，从Redis库存计算
                String stockKey = CacheConstants.FLASH_SALE_STOCK_PREFIX + item.getId();
                Object stockObj = redisUtil.get(stockKey);

                if (stockObj != null) {
                    // 如果Redis中有库存数据，计算销量
                    int currentStock = Integer.parseInt(stockObj.toString());
                    int originalStock = item.getStock() + (item.getSoldCount() == null ? 0 : item.getSoldCount());
                    if (originalStock <= 0) {
                        // 避免数据异常情况
                        originalStock = Math.max(1, item.getStock()); // 确保总库存至少为1
                    }

                    // 确保销量不为负数且不超过原始库存
                    int soldCount = Math.max(0, originalStock - currentStock);
                    // 如果库存为0且已有销量，则认为已售罄
                    if (currentStock == 0 && item.getStatus() == 1) {
                        soldCount = originalStock; // 全部售罄
                    }

                    item.setSoldCount(soldCount);

                    // 将计算出的销量缓存到Redis
                    redisUtil.set(soldCountKey, soldCount, CacheConstants.FLASH_SALE_ITEM_INFO_EXPIRE);
                } else {
                    // 3. 如果Redis中没有库存数据，检查是否需要预热库存
                    if (item.getStatus() == 1) { // 如果活动正在进行中，应该预热库存
                        log.warn("秒杀活动 {} 正在进行中但Redis中无库存数据，执行库存预热", item.getId());
                        preloadItemStock(item.getId(), item.getStock());
                        item.setSoldCount(0); // 新预热的库存，销量为0
                    } else {
                        // 4. 最后尝试从数据库获取销量（这里可能抛出异常，但已在try-catch中处理）
                        try {
                            int soldCount = flashSaleOrderMapper.countOrdersByItemId(item.getId());
                            item.setSoldCount(soldCount);
                            // 将数据库查询的销量缓存到Redis
                            redisUtil.set(soldCountKey, soldCount, CacheConstants.FLASH_SALE_ITEM_INFO_EXPIRE);
                        } catch (Exception e) {
                            log.error("查询商品销量失败: {}", e.getMessage());
                            item.setSoldCount(0); // 出错时设置销量为0
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 如果所有方法都失败，设置销量为0
            log.error("处理商品销量信息失败: {}", e.getMessage(), e);
            item.setSoldCount(0);
        }
    }

    /**
     * 根据查询条件筛选秒杀商品列表
     *
     * @param items    原始商品列表
     * @param queryDto 查询条件
     * @return 过滤后的商品列表
     */
    private List<FlashSaleItemVO> filterFlashSaleItems(List<FlashSaleItemVO> items, FlashSaleItemQueryDto queryDto) {
        if (CollectionUtils.isEmpty(items) || queryDto == null) {
            return items;
        }

        List<FlashSaleItemVO> result = new ArrayList<>();
        for (FlashSaleItemVO item : items) {
            // 过滤名称
            if (StringUtils.hasText(queryDto.getName()) &&
                    !item.getName().contains(queryDto.getName())) {
                continue;
            }

            // 过滤状态
            if (queryDto.getStatus() != null && !item.getStatus().equals(queryDto.getStatus())) {
                continue;
            }

            // 过滤开始时间
            if (queryDto.getStartTimeBegin() != null &&
                    item.getStartTime().isBefore(queryDto.getStartTimeBegin())) {
                continue;
            }

            if (queryDto.getStartTimeEnd() != null &&
                    item.getStartTime().isAfter(queryDto.getStartTimeEnd())) {
                continue;
            }

            // 过滤结束时间
            if (queryDto.getEndTimeBegin() != null &&
                    item.getEndTime().isBefore(queryDto.getEndTimeBegin())) {
                continue;
            }

            if (queryDto.getEndTimeEnd() != null &&
                    item.getEndTime().isAfter(queryDto.getEndTimeEnd())) {
                continue;
            }

            result.add(item);
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> endFlashSale(Integer itemId) {
        try {
            if (itemId == null) {
                return ApiResult.error("商品ID不能为空");
            }

            // 获取商品信息
            FlashSaleItem item = flashSaleItemMapper.selectById(itemId);
            if (item == null) {
                return ApiResult.error("商品不存在");
            }

            // 只有未结束的活动可以手动结束
            if (item.getStatus() == 2) {
                return ApiResult.error("该活动已经结束");
            }

            // 更新状态为已结束
            item.setStatus(2);
            item.setUpdateTime(LocalDateTime.now());
            item.setEndTime(LocalDateTime.now()); // 将结束时间设置为当前时间

            // 保存到数据库
            flashSaleItemMapper.updateById(item);

            // 清除缓存
            redisUtil.del(CacheConstants.FLASH_SALE_ITEM_LIST);
            redisUtil.del(CacheConstants.FLASH_SALE_ITEM_PREFIX + itemId);

            log.info("手动结束秒杀活动 ID: {}", itemId);

            return ApiResult.success();
        } catch (Exception e) {
            log.error("结束秒杀活动失败: {}", e.getMessage(), e);
            return ApiResult.error("结束秒杀活动失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> forceEndFlashSale(Integer itemId) {
        try {
            if (itemId == null) {
                return ApiResult.error("商品ID不能为空");
            }

            // 获取商品信息
            FlashSaleItem item = flashSaleItemMapper.selectById(itemId);
            if (item == null) {
                return ApiResult.error("商品不存在");
            }

            // 强制结束，无论状态如何
            item.setStatus(2);
            item.setUpdateTime(LocalDateTime.now());

            // 如果活动尚未结束，设置结束时间为当前时间
            if (item.getEndTime().isAfter(LocalDateTime.now())) {
                item.setEndTime(LocalDateTime.now());
            }

            // 保存到数据库
            flashSaleItemMapper.updateById(item);

            // 清除缓存
            redisUtil.del(CacheConstants.FLASH_SALE_ITEM_LIST);
            redisUtil.del(CacheConstants.FLASH_SALE_ITEM_PREFIX + itemId);

            log.info("强制结束秒杀活动 ID: {}", itemId);

            // 刷新状态
            refreshFlashSaleStatus();

            return ApiResult.success();
        } catch (Exception e) {
            log.error("强制结束秒杀活动失败: {}", e.getMessage(), e);
            return ApiResult.error("强制结束秒杀活动失败");
        }
    }

    /**
     * 发送秒杀商品发布通知给所有用户
     *
     * @param flashSaleItem 秒杀商品信息
     */
    private void sendFlashSalePublishNotification(FlashSaleItem flashSaleItem) {
        try {
            // 创建通知消息
            NotificationMessage notification = NotificationMessage.builder()
                    .type("flash_sale_publish")
                    .senderId(0) // 系统发送
                    .senderName("系统")
                    .receiverId(0) // 0表示广播给所有用户
                    .contentId(flashSaleItem.getId())
                    .contentType("flash_sale")
                    .title("🔥 秒杀商品上线通知")
                    .content(String.format("新的秒杀商品【%s】限时特价 ¥%.2f 上线啦！数量有限，先到先得！点击查看详情",
                            flashSaleItem.getName(),
                            flashSaleItem.getFlashPrice()))
                    .createTime(LocalDateTime.now())
                    .isRead(false)
                    .relatedData(String.format("{\"itemId\":%d,\"itemName\":\"%s\",\"price\":%.2f}",
                            flashSaleItem.getId(),
                            flashSaleItem.getName(),
                            flashSaleItem.getFlashPrice()))
                    .build();

            // 发送广播通知
            notificationService.sendNotification(notification);

            log.info("秒杀商品发布通知发送成功，商品: {}", flashSaleItem.getName());

        } catch (Exception e) {
            log.error("发送秒杀商品发布通知失败: {}", e.getMessage(), e);
            throw e;
        }
    }
}