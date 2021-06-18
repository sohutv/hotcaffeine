package com.hotcaffeine.metric;

import org.junit.Test;

import com.hotcaffeine.data.metric.HotKey;
import com.hotcaffeine.data.metric.TopK;

public class TopKTest {

    @Test
    public void test() {
        TopK<HotKey> topK = new TopK<>(100);
        topK.add(new HotKey(1000, 1000, "B"));
        for (int i = 0; i < 110; ++i) {
            topK.add(new HotKey(i, i, "key" + i));
        }
        topK.add(new HotKey(1000, 1000, "A"));
        System.out.println(topK);
    }

}
