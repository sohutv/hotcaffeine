package com.hotcaffeine.common.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;

import io.etcd.jetcd.shaded.com.google.common.hash.Hashing;

/**
 * 一致性hash server
 * 
 * @author yongfeigao
 * @date 2021年2月2日
 */
public class ConsistentHashServer {
    // 节点
    private SortedMap<Long, String> serverMap = new ConcurrentSkipListMap<>();
    // 虚拟节点数量
    private int numberOfVirtualNodes;
    // hash
    private HashFunction hashFunction;

    /**
     * 初始化
     */
    public ConsistentHashServer() {
        this(300);
    }
    
    /**
     * 初始化
     * 
     * @param numberOfVirtualNodes 虚拟节点数量
     */
    public ConsistentHashServer(int numberOfVirtualNodes) {
        this(numberOfVirtualNodes, new Murmur3HashFunction());
    }
    
    /**
     * 初始化
     * 
     * @param numberOfVirtualNodes 虚拟节点数量
     */
    public ConsistentHashServer(int numberOfVirtualNodes, HashFunction hashFunction) {
        this.numberOfVirtualNodes = numberOfVirtualNodes;
        this.hashFunction = hashFunction;
    }

    /**
     * 添加地址
     * 
     * @param address
     */
    public void add(String address) {
        for (int i = 0; i < numberOfVirtualNodes; i++) {
            long hash = hash(getVirtualAddress(address, i));
            serverMap.put(hash, address);
        }
    }

    /**
     * 删除地址
     * 
     * @param address
     */
    public void remove(String address) {
        for (int i = 0; i < numberOfVirtualNodes; i++) {
            long hash = hash(getVirtualAddress(address, i));
            serverMap.remove(hash);
        }
    }

    /**
     * 虚拟地址
     * 
     * @param address
     * @param i
     * @return
     */
    private String getVirtualAddress(String address, int i) {
        return address + "#" + i;
    }

    /**
     * hash函数
     * 
     * @param key
     * @return
     */
    public long hash(String key) {
        return hashFunction.hash(key);
    }

    /**
     * 选择节点
     * 
     * @param key
     * @return
     */
    public String select(String key) {
        if (serverMap.isEmpty()) {
            return null;
        }
        long hash = hash(key);
        SortedMap<Long, String> tailMap = serverMap.tailMap(hash);
        hash = tailMap.isEmpty() ? serverMap.firstKey() : tailMap.firstKey();
        return serverMap.get(hash);
    }

    public SortedMap<Long, String> getServerMap() {
        return serverMap;
    }
    
    public void clear() {
        serverMap.clear();
    }

    /**
     * hash函数
     * 
     * @author yongfeigao
     * @date 2021年2月3日
     */
    public static interface HashFunction {
        public long hash(String key);
    }

    /**
     * md5hash
     * 
     * @author yongfeigao
     * @date 2021年2月3日
     */
    public static class MD5HashFunction implements HashFunction {
        @Override
        public long hash(String key) {
            MessageDigest instance;
            try {
                instance = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalArgumentException(e);
            }
            instance.update(key.getBytes());
            byte[] digest = instance.digest();

            long h = 0;
            for (int i = 0; i < 4; i++) {
                h <<= 8;
                h |= ((int) digest[i]) & 0xFF;
            }
            return h;
        }
    }

    /**
     * jdk hash
     * 
     * @author yongfeigao
     * @date 2021年2月3日
     */
    public static class JDKHashFunction implements HashFunction {
        @Override
        public long hash(String key) {
            return key.hashCode() & 0x7FFFFFFF;
        }
    }

    /**
     * Murmur3 hash
     * 
     * @author yongfeigao
     * @date 2021年2月3日
     */
    public static class Murmur3HashFunction implements HashFunction {
        @Override
        public long hash(String key) {
            return Hashing.murmur3_128().hashBytes(key.getBytes()).asLong();
        }
    }
}