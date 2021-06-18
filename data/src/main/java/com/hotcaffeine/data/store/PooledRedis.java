package com.hotcaffeine.data.store;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.util.Pool;

/**
 * 池化的redis
 * 
 * @author yongfeigao
 * @date 2021年5月28日
 */
public class PooledRedis implements IRedis {

    private Pool<Jedis> pool;

    public PooledRedis() {
        this.pool = new JedisPool();
    }

    @Override
    public void init(RedisConfiguration redisConfiguration) {
        this.pool = new JedisPool(redisConfiguration.getPoolConfig(), 
                redisConfiguration.getHost(),
                redisConfiguration.getPort(), 
                redisConfiguration.getConnectionTimeout(),
                redisConfiguration.getSoTimeout(), 
                redisConfiguration.getPassword(),
                Protocol.DEFAULT_DATABASE, null);
    }

    @Override
    public Pool<Jedis> getPool() {
        return pool;
    }

    @Override
    public JedisCluster getJedisCluster() {
        return null;
    }

    @Override
    public Long incr(String key) {
        return execute(jedis -> jedis.incr(key));
    }

    @Override
    public Long lpush(String key, String... strings) {
        return execute(jedis -> jedis.lpush(key, strings));
    }

    @Override
    public Long ttl(String key) {
        return execute(jedis -> jedis.ttl(key));
    }

    @Override
    public String get(String key) {
        return execute(jedis -> jedis.get(key));
    }

    @Override
    public String set(String key, String value) {
        return execute(jedis -> jedis.set(key, value));
    }

    @Override
    public String set(String key, String value, SetParams params) {
        return execute(jedis -> jedis.set(key, value, params));
    }

    @Override
    public Long expire(String key, int seconds) {
        return execute(jedis -> jedis.expire(key, seconds));
    }

    @Override
    public Long llen(String key) {
        return execute(jedis -> jedis.llen(key));
    }

    @Override
    public List<String> lrange(String key, long start, long stop) {
        return execute(jedis -> jedis.lrange(key, start, stop));
    }

    @Override
    public Long rpush(String key, String... strings) {
        return execute(jedis -> jedis.rpush(key, strings));
    }

    @Override
    public Long del(String key) {
        return execute(jedis -> jedis.del(key));
    }

    @Override
    public String setex(String key, int seconds, String value) {
        return execute(jedis -> jedis.setex(key, seconds, value));
    }

    @Override
    public Long hset(String key, String field, String value) {
        return execute(jedis -> jedis.hset(key, field, value));
    }

    @Override
    public String hget(String key, String field) {
        return execute(jedis -> jedis.hget(key, field));
    }

    @Override
    public Map<String, String> hgetAll(String key) {
        return execute(jedis -> jedis.hgetAll(key));
    }

    @Override
    public Long hdel(String key, String... field) {
        return execute(jedis -> jedis.hdel(key, field));
    }

    @Override
    public Long zadd(String key, double score, String member) {
        return execute(jedis -> jedis.zadd(key, score, member));
    }

    @Override
    public Long zrem(String key, String... member) {
        return execute(jedis -> jedis.zrem(key, member));
    }

    @Override
    public Long zremrangeByRank(String key, long start, long end) {
        return execute(jedis -> jedis.zremrangeByRank(key, start, end));
    }

    @Override
    public Long zcard(String key) {
        return execute(jedis -> jedis.zcard(key));
    }

    @Override
    public Set<String> zrevrange(String key, long start, long stop) {
        return execute(jedis -> jedis.zrevrange(key, start, stop));
    }
    
    @Override
    public Long sadd(String key, String... member) {
        return execute(jedis -> jedis.sadd(key, member));
    }

    @Override
    public Set<String> smembers(String key) {
        return execute(jedis -> jedis.smembers(key));
    }

    private <R> R execute(Function<Jedis, R> function) {
        try (Jedis jedis = pool.getResource()) {
            return function.apply(jedis);
        }
    }

    public void setPool(Pool<Jedis> pool) {
        this.pool = pool;
    }
}
