package com.hotcaffeine.data.store;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.util.Pool;

/**
 * ClusterRedis
 * 
 * @author yongfeigao
 * @date 2021年5月28日
 */
public class ClusterRedis implements IRedis {

    protected JedisCluster jedisCluster;

    public ClusterRedis() {
    }

    @Override
    public void init(RedisConfiguration redisConfiguration) {
        String[] hostAndPortArray = redisConfiguration.getHost().split(",");
        Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
        for (String hostAndPort : hostAndPortArray) {
            String[] tmpArray = hostAndPort.split(":");
            jedisClusterNode.add(new HostAndPort(tmpArray[0], Integer.parseInt(tmpArray[1])));
        }
        jedisCluster = new JedisCluster(jedisClusterNode, redisConfiguration.getConnectionTimeout(),
                redisConfiguration.getSoTimeout(), 5, redisConfiguration.getPassword(),
                redisConfiguration.getPoolConfig());
    }

    @Override
    public Pool<Jedis> getPool() {
        return null;
    }

    @Override
    public JedisCluster getJedisCluster() {
        return jedisCluster;
    }

    @Override
    public Long ttl(String key) {
        return jedisCluster.ttl(key);
    }

    @Override
    public Long incr(String key) {
        return jedisCluster.incr(key);
    }

    @Override
    public Long expire(String key, int seconds) {
        return jedisCluster.expire(key, seconds);
    }

    @Override
    public Long del(String key) {
        return jedisCluster.del(key);
    }

    @Override
    public String get(String key) {
        return jedisCluster.get(key);
    }

    @Override
    public String set(String key, String value) {
        return jedisCluster.set(key, value);
    }

    @Override
    public String set(String key, String value, SetParams params) {
        return jedisCluster.set(key, value, params);
    }

    @Override
    public String setex(String key, int seconds, String value) {
        return jedisCluster.setex(key, seconds, value);
    }

    @Override
    public Long llen(String key) {
        return jedisCluster.llen(key);
    }

    @Override
    public Long lpush(String key, String... strings) {
        return jedisCluster.lpush(key, strings);
    }

    @Override
    public List<String> lrange(String key, long start, long stop) {
        return jedisCluster.lrange(key, start, stop);
    }

    @Override
    public Long rpush(String key, String... strings) {
        return jedisCluster.rpush(key, strings);
    }

    @Override
    public Long hset(String key, String field, String value) {
        return jedisCluster.hset(key, field, value);
    }

    @Override
    public String hget(String key, String field) {
        return jedisCluster.hget(key, field);
    }

    @Override
    public Map<String, String> hgetAll(String key) {
        return jedisCluster.hgetAll(key);
    }

    @Override
    public Long hdel(String key, String... field) {
        return jedisCluster.hdel(key, field);
    }

    @Override
    public Long zadd(String key, double score, String member) {
        return jedisCluster.zadd(key, score, member);
    }

    @Override
    public Long zrem(String key, String... member) {
        return jedisCluster.zrem(key, member);
    }

    @Override
    public Long zremrangeByRank(String key, long start, long end) {
        return jedisCluster.zremrangeByRank(key, start, end);
    }

    @Override
    public Long zcard(String key) {
        return jedisCluster.zcard(key);
    }

    @Override
    public Set<String> zrevrange(String key, long start, long stop) {
        return jedisCluster.zrevrange(key, start, stop);
    }

    @Override
    public Long sadd(String key, String... member) {
        return jedisCluster.sadd(key, member);
    }

    @Override
    public Set<String> smembers(String key) {
        return jedisCluster.smembers(key);
    }
}
