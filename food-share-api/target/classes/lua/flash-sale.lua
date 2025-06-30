-- 秒杀原子操作Lua脚本
-- KEYS[1]: 库存key
-- KEYS[2]: 锁key  
-- KEYS[3]: 用户记录key
-- ARGV[1]: 用户ID
-- ARGV[2]: 锁过期时间
-- ARGV[3]: 用户记录过期时间

-- 检查用户是否已经参与过秒杀
if redis.call('EXISTS', KEYS[3]) == 1 then
    return {-1, '用户已参与过此秒杀'}
end

-- 尝试获取分布式锁
local lockResult = redis.call('SET', KEYS[2], ARGV[1], 'PX', ARGV[2], 'NX')
if not lockResult then
    return {-2, '系统繁忙，请稍后再试'}
end

-- 检查库存
local stock = redis.call('GET', KEYS[1])
if not stock or tonumber(stock) <= 0 then
    -- 释放锁
    redis.call('DEL', KEYS[2])
    return {-3, '商品已售罄'}
end

-- 扣减库存
local remainingStock = redis.call('DECR', KEYS[1])
if remainingStock < 0 then
    -- 库存不足，恢复库存并释放锁
    redis.call('INCR', KEYS[1])
    redis.call('DEL', KEYS[2])
    return {-3, '商品已售罄'}
end

-- 记录用户参与记录
redis.call('SET', KEYS[3], ARGV[1], 'EX', ARGV[3])

-- 释放锁
redis.call('DEL', KEYS[2])

return {1, '秒杀成功', remainingStock}
