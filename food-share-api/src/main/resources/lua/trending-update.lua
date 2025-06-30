-- 热门排行榜更新Lua脚本
-- KEYS[1]: 排行榜key
-- KEYS[2]: UV统计key
-- ARGV[1]: 美食ID
-- ARGV[2]: 用户ID
-- ARGV[3]: 排行榜过期时间
-- ARGV[4]: UV过期时间

-- 记录UV
local uvAdded = redis.call('PFADD', KEYS[2], ARGV[2])

-- 如果是新用户访问，更新排行榜
if uvAdded == 1 then
    redis.call('ZINCRBY', KEYS[1], 1, ARGV[1])
    redis.call('EXPIRE', KEYS[1], ARGV[3])
end

-- 设置UV过期时间
redis.call('EXPIRE', KEYS[2], ARGV[4])

-- 获取当前UV数量
local uvCount = redis.call('PFCOUNT', KEYS[2])

-- 获取排行榜中的分数
local score = redis.call('ZSCORE', KEYS[1], ARGV[1])

return {uvCount, score or 0}
