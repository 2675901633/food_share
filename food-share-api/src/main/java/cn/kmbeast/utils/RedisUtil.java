package cn.kmbeast.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.LinkedHashMap;
import java.util.HashSet;

/**
 * Redis工具类
 */
@Component
public class RedisUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 指定缓存失效时间
     * 
     * @param key  键
     * @param time 时间(秒)
     * @return 是否成功
     */
    public boolean expire(String key, long time) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, TimeUnit.SECONDS);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 根据key获取过期时间
     * 
     * @param key 键
     * @return 时间(秒) 返回0代表为永久有效
     */
    public long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 判断key是否存在
     * 
     * @param key 键
     * @return true 存在 false不存在
     */
    public boolean hasKey(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除缓存
     * 
     * @param key 可以传一个或多个值
     */
    @SuppressWarnings("unchecked")
    public void del(String... key) {
        if (key != null && key.length > 0) {
            if (key.length == 1) {
                redisTemplate.delete(key[0]);
            } else {
                redisTemplate.delete((Collection<String>) CollectionUtils.arrayToList(key));
            }
        }
    }

    /**
     * 删除指定前缀的key
     * 
     * @param prefix 前缀
     */
    public void delByPrefix(String prefix) {
        Set<String> keys = redisTemplate.keys(prefix + "*");
        if (!CollectionUtils.isEmpty(keys)) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * 普通缓存获取
     * 
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    /**
     * 普通缓存放入
     * 
     * @param key   键
     * @param value 值
     * @return true成功 false失败
     */
    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 普通缓存放入并设置时间
     * 
     * @param key   键
     * @param value 值
     * @param time  时间(秒) time要大于0 如果time小于等于0 将设置无限期
     * @return true成功 false 失败
     */
    public boolean set(String key, Object value, long time) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 递增
     * 
     * @param key   键
     * @param delta 要增加几(大于0)
     * @return 递增后的值
     */
    public long incr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递增因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 递减
     * 
     * @param key   键
     * @param delta 要减少几(小于0)
     * @return 递减后的值
     */
    public long decr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递减因子必须大于0");
        }
        return redisTemplate.opsForValue().decrement(key, delta);
    }

    // ============================== Hash操作 ==============================

    /**
     * HashGet
     * 
     * @param key  键
     * @param item 项
     * @return 值
     */
    public Object hget(String key, String item) {
        return redisTemplate.opsForHash().get(key, item);
    }

    /**
     * 获取hashKey对应的所有键值
     * 
     * @param key 键
     * @return 对应的多个键值
     */
    public Map<Object, Object> hmget(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * HashSet
     * 
     * @param key 键
     * @param map 对应多个键值
     * @return true 成功 false 失败
     */
    public boolean hmset(String key, Map<String, Object> map) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * HashSet 并设置时间
     * 
     * @param key  键
     * @param map  对应多个键值
     * @param time 时间(秒)
     * @return true成功 false失败
     */
    public boolean hmset(String key, Map<String, Object> map, long time) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 向一张hash表中放入数据,如果不存在将创建
     * 
     * @param key   键
     * @param item  项
     * @param value 值
     * @return true 成功 false失败
     */
    public boolean hset(String key, String item, Object value) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 向一张hash表中放入数据,如果不存在将创建，并设置时间
     * 
     * @param key   键
     * @param item  项
     * @param value 值
     * @param time  时间(秒) 注意:如果已存在的hash表有时间，这里将会覆盖原有的时间
     * @return true 成功 false失败
     */
    public boolean hset(String key, String item, Object value, long time) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除hash表中的值
     * 
     * @param key  键
     * @param item 项 可以多个
     */
    public void hdel(String key, Object... item) {
        redisTemplate.opsForHash().delete(key, item);
    }

    /**
     * 判断hash表中是否有该项的值
     * 
     * @param key  键
     * @param item 项
     * @return true 存在 false不存在
     */
    public boolean hHasKey(String key, String item) {
        return redisTemplate.opsForHash().hasKey(key, item);
    }

    /**
     * hash递增
     * 
     * @param key  键
     * @param item 项
     * @param by   要增加几(大于0)
     * @return 递增后的值
     */
    public double hincr(String key, String item, double by) {
        return redisTemplate.opsForHash().increment(key, item, by);
    }

    /**
     * hash递减
     * 
     * @param key  键
     * @param item 项
     * @param by   要减少几(小于0)
     * @return 递减后的值
     */
    public double hdecr(String key, String item, double by) {
        return redisTemplate.opsForHash().increment(key, item, -by);
    }

    // ============================== Set操作 ==============================

    /**
     * 根据key获取Set中的所有值
     * 
     * @param key 键
     * @return Set
     */
    public Set<Object> sGet(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 查询value在Set中是否存在
     * 
     * @param key   键
     * @param value 值
     * @return true 存在 false不存在
     */
    public boolean sHasKey(String key, Object value) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将数据放入set缓存
     * 
     * @param key    键
     * @param values 值 可以是多个
     * @return 成功个数
     */
    public long sSet(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().add(key, values);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 将set数据放入缓存，并设置过期时间
     * 
     * @param key    键
     * @param time   时间(秒)
     * @param values 值 可以是多个
     * @return 成功个数
     */
    public long sSetAndTime(String key, long time, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().add(key, values);
            if (time > 0) {
                expire(key, time);
            }
            return count;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 获取set缓存的长度
     * 
     * @param key 键
     * @return 长度
     */
    public long sGetSetSize(String key) {
        try {
            return redisTemplate.opsForSet().size(key);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 移除值为value的
     * 
     * @param key    键
     * @param values 值 可以是多个
     * @return 移除的个数
     */
    public long setRemove(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().remove(key, values);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    // ============================== ZSet操作 ==============================

    /**
     * 添加元素到ZSet
     * 
     * @param key   键
     * @param value 值
     * @param score 分数
     * @return 是否成功
     */
    public boolean zAdd(String key, Object value, double score) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForZSet().add(key, value, score));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取ZSet指定区间的元素
     * 
     * @param key   键
     * @param start 开始索引
     * @param end   结束索引
     * @return 元素集合
     */
    public Set<Object> zRange(String key, long start, long end) {
        try {
            return redisTemplate.opsForZSet().range(key, start, end);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取ZSet指定分数区间的元素
     * 
     * @param key 键
     * @param min 最小分数
     * @param max 最大分数
     * @return 元素集合
     */
    public Set<Object> zRangeByScore(String key, double min, double max) {
        try {
            return redisTemplate.opsForZSet().rangeByScore(key, min, max);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取元素在ZSet中的排名
     * 
     * @param key   键
     * @param value 值
     * @return 排名，从0开始
     */
    public Long zRank(String key, Object value) {
        try {
            return redisTemplate.opsForZSet().rank(key, value);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取元素在ZSet中的分数
     * 
     * @param key   键
     * @param value 值
     * @return 分数
     */
    public Double zScore(String key, Object value) {
        try {
            return redisTemplate.opsForZSet().score(key, value);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ============================== List操作 ==============================

    /**
     * 获取list缓存的内容
     * 
     * @param key   键
     * @param start 开始索引
     * @param end   结束索引 0到-1代表所有值
     * @return List
     */
    public List<Object> lGet(String key, long start, long end) {
        try {
            return redisTemplate.opsForList().range(key, start, end);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取list缓存的长度
     * 
     * @param key 键
     * @return 长度
     */
    public long lGetListSize(String key) {
        try {
            return redisTemplate.opsForList().size(key);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 通过索引获取list中的值
     * 
     * @param key   键
     * @param index 索引 index>=0时，0表头，1第二个元素，依次类推；index<0时，-1表尾，-2倒数第二个元素，依次类推
     * @return 元素
     */
    public Object lGetIndex(String key, long index) {
        try {
            return redisTemplate.opsForList().index(key, index);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 将list放入缓存
     * 
     * @param key   键
     * @param value 值
     * @return 是否成功
     */
    public boolean lSet(String key, Object value) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将list放入缓存，并设置过期时间
     * 
     * @param key   键
     * @param value 值
     * @param time  时间(秒)
     * @return 是否成功
     */
    public boolean lSet(String key, Object value, long time) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将list放入缓存
     * 
     * @param key   键
     * @param value 值
     * @return 是否成功
     */
    public boolean lSet(String key, List<Object> value) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将list放入缓存，并设置过期时间
     * 
     * @param key   键
     * @param value 值
     * @param time  时间(秒)
     * @return 是否成功
     */
    public boolean lSet(String key, List<Object> value, long time) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 根据索引修改list中的某条数据
     * 
     * @param key   键
     * @param index 索引
     * @param value 值
     * @return 是否成功
     */
    public boolean lUpdateIndex(String key, long index, Object value) {
        try {
            redisTemplate.opsForList().set(key, index, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 移除N个值为value的元素
     * 
     * @param key   键
     * @param count 移除多少个
     * @param value 值
     * @return 移除的个数
     */
    public long lRemove(String key, long count, Object value) {
        try {
            return redisTemplate.opsForList().remove(key, count, value);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 向ZSet中添加元素
     *
     * @param key   键
     * @param value 值
     * @param score 分数
     * @return true成功 false失败
     */
    public boolean zAdd(String key, String value, double score) {
        try {
            return redisTemplate.opsForZSet().add(key, value, score);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取ZSet的大小
     *
     * @param key 键
     * @return ZSet大小
     */
    public long zSize(String key) {
        try {
            Long size = redisTemplate.opsForZSet().size(key);
            return size != null ? size : 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 移除ZSet中指定范围的元素
     *
     * @param key   键
     * @param start 开始索引
     * @param end   结束索引
     * @return 移除的元素数量
     */
    public long zRemoveRange(String key, long start, long end) {
        try {
            Long count = redisTemplate.opsForZSet().removeRange(key, start, end);
            return count != null ? count : 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 获取ZSet中指定范围的元素（按分数从高到低）
     *
     * @param key   键
     * @param start 开始索引
     * @param end   结束索引
     * @return 元素集合
     */
    public Set<String> zReverseRange(String key, long start, long end) {
        try {
            Set<Object> set = redisTemplate.opsForZSet().reverseRange(key, start, end);
            if (set == null) {
                return Collections.emptySet();
            }
            return set.stream()
                    .map(Object::toString)
                    .collect(java.util.stream.Collectors.toSet());
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptySet();
        }
    }

    /**
     * 获取ZSet中指定范围的元素及其分数（按分数从低到高）
     *
     * @param key 键
     * @return 元素与分数集合
     */
    public Set<String> zRangeWithScores(String key) {
        try {
            Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object>> tuples = redisTemplate
                    .opsForZSet().rangeWithScores(key, 0, -1);
            if (tuples == null) {
                return Collections.emptySet();
            }
            return tuples.stream()
                    .map(tuple -> tuple.getValue() + ":" + tuple.getScore())
                    .collect(java.util.stream.Collectors.toSet());
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptySet();
        }
    }

    /**
     * 向Hash中添加字段
     *
     * @param key   键
     * @param field 字段
     * @param value 值
     * @return true成功 false失败
     */
    public boolean hSet(String key, String field, Object value) {
        try {
            redisTemplate.opsForHash().put(key, field, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取Hash中指定字段的值
     *
     * @param key   键
     * @param field 字段
     * @return 值
     */
    public Object hGet(String key, String field) {
        try {
            return redisTemplate.opsForHash().get(key, field);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取Hash中所有字段和值
     *
     * @param key 键
     * @return 字段和值的映射
     */
    public Map<Object, Object> hGetAll(String key) {
        try {
            return redisTemplate.opsForHash().entries(key);
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }

    /**
     * 模糊查询键
     *
     * @param pattern 模式
     * @return 匹配的键集合
     */
    public Set<String> keys(String pattern) {
        try {
            return redisTemplate.keys(pattern);
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptySet();
        }
    }

    /**
     * HyperLogLog添加元素
     * 
     * @param key   键
     * @param value 值
     * @return 是否成功
     */
    public boolean pfAdd(String key, Object... value) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForHyperLogLog().add(key, value));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * HyperLogLog获取基数估计值
     * 
     * @param key 键
     * @return 基数估计值
     */
    public long pfCount(String key) {
        try {
            return redisTemplate.opsForHyperLogLog().size(key);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * HyperLogLog合并多个键
     * 
     * @param destKey    目标键
     * @param sourceKeys 源键数组
     * @return 合并后的基数估计值
     */
    public long pfMerge(String destKey, String... sourceKeys) {
        try {
            return redisTemplate.opsForHyperLogLog().union(destKey, sourceKeys);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 获取ZSet中指定排名范围的元素（按分数从高到低）
     *
     * @param key   键
     * @param start 开始排名（0表示第一名）
     * @param end   结束排名
     * @return 元素及其分数的映射
     */
    public Map<Object, Double> zReverseRangeWithScores(String key, long start, long end) {
        try {
            Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object>> tuples = redisTemplate
                    .opsForZSet().reverseRangeWithScores(key, start, end);
            if (tuples == null) {
                return Collections.emptyMap();
            }

            Map<Object, Double> result = new LinkedHashMap<>();
            for (org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object> tuple : tuples) {
                result.put(tuple.getValue(), tuple.getScore());
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }

    /**
     * 向ZSet中添加多个元素
     *
     * @param key    键
     * @param values 值-分数映射
     * @return 添加成功的元素数量
     */
    public long zAddAll(String key, Map<Object, Double> values) {
        try {
            Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object>> tuples = new HashSet<>();
            values.forEach((k, v) -> {
                tuples.add(new org.springframework.data.redis.core.DefaultTypedTuple<>(k, v));
            });
            Long count = redisTemplate.opsForZSet().add(key, tuples);
            return count != null ? count : 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}