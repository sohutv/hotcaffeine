package com.hotcaffeine.common.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

import org.junit.Assert;
import org.junit.Test;

import com.hotcaffeine.common.model.ConsistentHashServer.HashFunction;
import com.hotcaffeine.common.model.ConsistentHashServer.JDKHashFunction;
import com.hotcaffeine.common.model.ConsistentHashServer.MD5HashFunction;
import com.hotcaffeine.common.model.ConsistentHashServer.Murmur3HashFunction;

public class ConsistentHashServerTest {
    
    @Test
    public void testChoose() {
        ConsistentHashServer consistentHashServer = new ConsistentHashServer();
        consistentHashServer.add("127.0.0.1");
        consistentHashServer.add("127.0.0.2");
        consistentHashServer.add("127.0.0.3");
        consistentHashServer.add("127.0.0.4");
        System.out.println(consistentHashServer.select("localCache-CacheServiceImpl-getVideoBaseInfoObject-243601916"));
        
        consistentHashServer.remove("127.0.0.2");
        System.out.println(consistentHashServer.select("localCache-CacheServiceImpl-getVideoBaseInfoObject-243601916"));
        
        consistentHashServer.add("127.0.0.2");
        System.out.println(consistentHashServer.select("localCache-CacheServiceImpl-getVideoBaseInfoObject-243601916"));
        
        consistentHashServer.remove("127.0.0.4");
        System.out.println(consistentHashServer.select("localCache-CacheServiceImpl-getVideoBaseInfoObject-243601916"));
        
        consistentHashServer.add("127.0.0.4");
        System.out.println(consistentHashServer.select("localCache-CacheServiceImpl-getVideoBaseInfoObject-243601916"));
    }

    @Test
    public void test() {
        List<String> adressList = new ArrayList<>();
        adressList.add("127.0.0.5");
        adressList.add("127.0.0.6");
        adressList.add("127.0.0.7");
        adressList.add("127.0.0.8");
        adressList.add("127.0.0.9");
        adressList.add("127.1.0.1");
        adressList.add("127.1.0.2");
        adressList.add("127.1.0.3");
        adressList.add("127.1.0.4");
        adressList.add("127.1.0.5");
        int numberOfVirtualNodes = 300;

        Map<String, Long> hashMap = new HashMap<>();
        ConsistentHashServer consistentHash = new ConsistentHashServer(numberOfVirtualNodes);
        adressList.forEach(address->{
            consistentHash.add(address); 
        });
        HashMap<String, LongAdder> map = new HashMap<>();
        long times = 1000000;
        String prefix = "video";
        for (int i = 0; i < times; i++) {
            String key = prefix + i;
            hashMap.put(key, consistentHash.hash(key));
            map.computeIfAbsent(consistentHash.select(key), k -> new LongAdder()).increment();
        }
        Assert.assertEquals(times, hashMap.size());
        double avg = times / adressList.size();
        map.forEach((k, v) -> {
            System.out.println(k + "与平均值差比" + minusRate(v.longValue(), avg) + "%");
        });
        System.out.println("标准差：" + standardDiviation(map) + ",平均值：" + times / adressList.size());

        // 迁移比率
        ConsistentHashServer consistentHash2 = new ConsistentHashServer(numberOfVirtualNodes);
        adressList.forEach(address->{
            consistentHash2.add(address); 
        });
        consistentHash2.remove("127.0.0.5");
        Assert.assertEquals((adressList.size() - 1) * numberOfVirtualNodes, consistentHash2.getServerMap().size());
        long notEqual = 0;
        for (int i = 0; i < times; i++) {
            String key = prefix + i;
            Assert.assertEquals(hashMap.get(key).longValue(), consistentHash.hash(key));
            if (!consistentHash.select(key).equals(consistentHash2.select(key))) {
                ++notEqual;
            }
        }
        System.out.println("迁移比例：" + rate(notEqual, times) + "%");

        List<String> list2 = new ArrayList<>(adressList);
        list2.remove(0);
        // 普通迁移比例
        notEqual = 0;
        for (int i = 0; i < times; i++) {
            String key = prefix + i;
            int index = Math.abs(key.hashCode() % adressList.size());
            int index2 = Math.abs(key.hashCode() % list2.size());
            if (!adressList.get(index).equals(list2.get(index2))) {
                ++notEqual;
            }
        }
        System.out.println("普通迁移比例：" + rate(notEqual, times) + "%");
    }

    private double minusRate(double n, double n2) {
        return rate(Math.abs(n - n2), n2);
    }

    private double rate(double n, double n2) {
        double rate = n / n2;
        return (long) (rate * 100) / 100D * 100;
    }

    public static double standardDiviation(Map<String, LongAdder> map) {
        int size = map.size();
        // 求和
        long sum = map.values().stream().mapToLong(LongAdder::longValue).sum();
        // 求平均值
        double dAve = sum / size;
        double dVar = 0;
        // 求方差
        for (Object key : map.keySet()) {
            long value = map.get(key).longValue();
            dVar += (value - dAve) * (value - dAve);
        }
        return Math.sqrt(dVar / size);
    }

    @Test
    public void testDifferentHash() {
        System.out.println("===MD5HashFunction");
        test(new MD5HashFunction());
        System.out.println("===JDKHashFunction");
        test(new JDKHashFunction());
        System.out.println("===Murmur3HashFunction");
        test(new Murmur3HashFunction());
    }
    
    private void test(HashFunction hashFunction) {
        List<String> adressList = new ArrayList<>();
        adressList.add("127.0.0.5");
        adressList.add("127.0.0.6");
        adressList.add("127.0.0.7");
        adressList.add("127.0.0.8");
        adressList.add("127.0.0.9");
        adressList.add("127.1.0.1");
        adressList.add("127.1.0.2");
        adressList.add("127.1.0.3");
        adressList.add("127.1.0.4");
        adressList.add("127.1.0.5");
        int numberOfVirtualNodes = 300;
        Map<String, Long> hashMap = new HashMap<>();
        ConsistentHashServer consistentHash = new ConsistentHashServer(numberOfVirtualNodes, hashFunction);
        adressList.forEach(address->{
            consistentHash.add(address); 
        });
        HashMap<String, LongAdder> map = new HashMap<>();
        long times = 1000000;
        String prefix = "video";
        for (int i = 0; i < times; i++) {
            String key = prefix + i;
            hashMap.put(key, consistentHash.hash(key));
            map.computeIfAbsent(consistentHash.select(key), k -> new LongAdder()).increment();
        }
        Assert.assertEquals(times, hashMap.size());
        double avg = times / adressList.size();
        map.forEach((k, v) -> {
            System.out.println(k + "与平均值差比" + minusRate(v.longValue(), avg) + "%");
        });
        System.out.println("标准差：" + standardDiviation(map) + ",平均值：" + times / adressList.size());
    }
}
