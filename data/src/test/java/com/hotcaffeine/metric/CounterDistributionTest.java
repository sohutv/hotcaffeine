package com.hotcaffeine.metric;

import org.junit.Assert;
import org.junit.Test;

import com.hotcaffeine.data.metric.CounterDistribution;

public class CounterDistributionTest {

    @Test
    public void test() {
        CounterDistribution counterDistribution = new CounterDistribution();
        Assert.assertEquals(1, counterDistribution.hash(1));
        Assert.assertEquals(3, counterDistribution.hash(3));
        Assert.assertEquals(22, counterDistribution.hash(22));
        Assert.assertEquals(59, counterDistribution.hash(59));
        Assert.assertEquals(99, counterDistribution.hash(99));
        Assert.assertEquals(100, counterDistribution.hash(100));
        
        Assert.assertEquals(105, counterDistribution.hash(101));
        Assert.assertEquals(105, counterDistribution.hash(104));
        Assert.assertEquals(105, counterDistribution.hash(105));
        
        Assert.assertEquals(110, counterDistribution.hash(106));
        Assert.assertEquals(210, counterDistribution.hash(209));
        Assert.assertEquals(210, counterDistribution.hash(210));
        Assert.assertEquals(215, counterDistribution.hash(211));
        
        Assert.assertEquals(495, counterDistribution.hash(494));
        Assert.assertEquals(495, counterDistribution.hash(495));
        Assert.assertEquals(500, counterDistribution.hash(496));
        Assert.assertEquals(500, counterDistribution.hash(499));
        Assert.assertEquals(500, counterDistribution.hash(500));
        
        Assert.assertEquals(510, counterDistribution.hash(501));
        Assert.assertEquals(510, counterDistribution.hash(502));
        Assert.assertEquals(510, counterDistribution.hash(509));
        Assert.assertEquals(510, counterDistribution.hash(510));
        
        Assert.assertEquals(900, counterDistribution.hash(900));
        Assert.assertEquals(910, counterDistribution.hash(901));
        Assert.assertEquals(910, counterDistribution.hash(909));
        
        Assert.assertEquals(990, counterDistribution.hash(990));
        Assert.assertEquals(1000, counterDistribution.hash(991));
        Assert.assertEquals(1000, counterDistribution.hash(999));
        Assert.assertEquals(1000, counterDistribution.hash(1000));
        
        Assert.assertEquals(1100, counterDistribution.hash(1001));
        Assert.assertEquals(1100, counterDistribution.hash(1050));
        Assert.assertEquals(1100, counterDistribution.hash(1090));
        Assert.assertEquals(1100, counterDistribution.hash(1099));
        Assert.assertEquals(1100, counterDistribution.hash(1100));
        
        
        Assert.assertEquals(1000100, counterDistribution.hash(1000001));
        Assert.assertEquals(1000100, counterDistribution.hash(1000090));
        Assert.assertEquals(1000100, counterDistribution.hash(1000099));
        Assert.assertEquals(1000100, counterDistribution.hash(1000100));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testError() {
        CounterDistribution counterDistribution = new CounterDistribution();
        counterDistribution.incr(0);
    }
    
    @Test
    public void testIncr() {
        CounterDistribution counterDistribution = new CounterDistribution();
        incr(counterDistribution, 1, 1000);
        incr(counterDistribution, 2, 900);
        incr(counterDistribution, 3, 500);
        incr(counterDistribution, 22, 100);
        incr(counterDistribution, 33, 10);
        incr(counterDistribution, 99, 10);
        incr(counterDistribution, 102, 9);
        incr(counterDistribution, 212, 4);
        System.out.println(counterDistribution.toString());
    }
    
    private void incr(CounterDistribution counterDistribution, int counter, int times) {
        for(int i = 0; i < times; ++i) {
            counterDistribution.incr(counter, 100);
        }
    }
    
    @Test
    public void testRemove() {
        CounterDistribution counterDistribution = new CounterDistribution();
        counterDistribution.setMaxSize(4);
        incr(counterDistribution, 1, 1000);
        incr(counterDistribution, 2, 900);
        incr(counterDistribution, 3, 500);
        incr(counterDistribution, 22, 100);
        incr(counterDistribution, 33, 10);
        incr(counterDistribution, 99, 10);
        incr(counterDistribution, 102, 9);
        incr(counterDistribution, 212, 4);
        System.out.println(counterDistribution.toString());
    }
}
