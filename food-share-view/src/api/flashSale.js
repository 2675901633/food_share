import request from '@/utils/request';

/**
 * 查询秒杀商品列表
 * @param {Object} data 查询参数
 * @returns {Promise<any>}
 */
export function listFlashSaleItems(data) {
    return request({
        url: '/flashSale/list',
        method: 'post',
        data
    });
}

/**
 * 获取秒杀商品详情
 * @param {number} itemId 商品ID
 * @returns {Promise<any>}
 */
export function getFlashSaleItemDetail(itemId) {
    return request({
        url: `/flashSale/detail/${itemId}`,
        method: 'get'
    });
}

/**
 * 执行秒杀
 * @param {number} itemId 商品ID
 * @returns {Promise<any>}
 */
export function doFlashSale(itemId) {
    return request({
        url: `/flashSale/doFlashSale/${itemId}`,
        method: 'post'
    });
}

/**
 * 查询用户订单
 * @returns {Promise<any>}
 */
export function queryMyOrders() {
    return request({
        url: '/flashSale/myOrders',
        method: 'get'
    });
}

/**
 * 取消订单
 * @param {string} orderId 订单ID
 * @returns {Promise<any>}
 */
export function cancelOrder(orderId) {
    return request({
        url: `/flashSale/cancelOrder/${orderId}`,
        method: 'post'
    });
}

/**
 * 创建秒杀商品
 * @param {Object} data 商品数据
 * @returns {Promise<any>}
 */
export function createFlashSaleItem(data) {
    return request({
        url: '/flashSale/admin/create',
        method: 'post',
        data
    });
}

/**
 * 更新秒杀商品
 * @param {Object} data 商品数据
 * @returns {Promise<any>}
 */
export function updateFlashSaleItem(data) {
    return request({
        url: '/flashSale/admin/update',
        method: 'post',
        data
    });
}

/**
 * 删除秒杀商品
 * @param {number} itemId 商品ID
 * @returns {Promise<any>}
 */
export function deleteFlashSaleItem(itemId) {
    return request({
        url: `/flashSale/admin/delete/${itemId}`,
        method: 'post'
    });
}

/**
 * 预热秒杀商品库存
 * @returns {Promise<any>}
 */
export function preloadFlashSaleStock() {
    return request({
        url: '/flashSale/admin/preload',
        method: 'post'
    });
}

/**
 * 刷新秒杀商品状态
 * @returns {Promise<any>}
 */
export function refreshFlashSaleStatus() {
    return request({
        url: '/flashSale/admin/refresh',
        method: 'post'
    });
}

/**
 * 获取所有秒杀订单
 * @param {Object} data 查询参数
 * @returns {Promise<any>}
 */
export function getAllOrders(data) {
    return request({
        url: '/flashSale/orders',
        method: 'post',
        data
    });
}

/**
 * 支付订单
 * @param {string} orderId 订单ID
 * @returns {Promise<any>}
 */
export function payOrder(orderId) {
    return request({
        url: `/flashSale/payOrder/${orderId}`,
        method: 'post'
    });
}

/**
 * 手动结束秒杀活动
 * @param {number} itemId 商品ID
 * @returns {Promise<any>}
 */
export function endFlashSale(itemId) {
    // 首先尝试专门的结束API
    return request({
        url: `/flashSale/admin/endSale/${itemId}`,
        method: 'post',
        // 当请求失败时，不会自动重试，让调用方处理错误
        validateStatus: function (status) {
            return status >= 200 && status < 500; // 默认值，返回true表示接受任何状态码
        }
    });
}

/**
 * 强制结束秒杀活动 - 仅更新状态，用于备份方法
 * @param {Object} data 包含id和status的对象
 * @returns {Promise<any>}
 */
export function forceEndFlashSale(data) {
    return request({
        url: `/flashSale/admin/forceEndSale`,
        method: 'post',
        data
    });
} 