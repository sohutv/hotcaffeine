package com.hotcaffeine.worker.task;

import org.junit.Test;

import com.hotcaffeine.worker.task.CounterDistributionTask;

public class CounterDistributionTaskTest {

    @Test
    public void test() {
        CounterDistributionTask counterDistributionTask = new CounterDistributionTask();
        counterDistributionTask.collect();
    }

}
