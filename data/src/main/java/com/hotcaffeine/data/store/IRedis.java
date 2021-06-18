package com.hotcaffeine.data.store;

import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.util.Pool;

/**
 * redis统一接口，屏蔽底层实现，所有方法都来自于jedis
 * 
 * @author yongfeigao
 * @date 2021年5月28日
 */
public interface IRedis {
    /**
     * 初始化
     * 
     * @param redisConfiguration
     */
    void init(RedisConfiguration redisConfiguration);
    
    Pool<Jedis> getPool();

    JedisCluster getJedisCluster();
    
    Long ttl(String key);

    Long expire(String key, int seconds);

    Long del(String key);
    
    Long incr(String key);

    String get(String key);

    String set(String key, String value);

    String setex(String key, int seconds, String value);

    String set(String key, String value, SetParams params);

    Long llen(String key);

    Long lpush(String key, String... strings);

    List<String> lrange(String key, long start, long stop);

    Long rpush(String key, String... strings);

    Long hset(String key, String field, String value);

    Map<String, String> hgetAll(String key);
    
    String hget(String key, String field);
    
    Long hdel(String key, String... field);

    Long zadd(String key, double score, String member);

    Long zrem(String key, String... member);

    Long zremrangeByRank(String key, long start, long end);

    Long zcard(String key);

    Set<String> zrevrange(String key, long start, long stop);
    
    Long sadd(String key, String... member);
    
    Set<String> smembers(String key);
}
