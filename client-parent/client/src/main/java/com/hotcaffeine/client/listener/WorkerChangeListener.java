package com.hotcaffeine.client.listener;

import java.util.Set;

import com.hotcaffeine.client.netty.NettyClient;

import io.etcd.jetcd.shaded.com.google.common.eventbus.Subscribe;

/**
 * eventbus监听worker信息变动
 *
 * @author wuweifeng wrote on 2020-01-13
 * @version 1.0
 */
public class WorkerChangeListener {
    
    private NettyClient nettyClient;

    public WorkerChangeListener(NettyClient nettyClient) {
        this.nettyClient = nettyClient;
    }

    /**
     * 监听worker信息变动
     */
    @Subscribe
    public void connectAll(WorkerChangeEvent event) {
        nettyClient.connect(event.getAddresses());
    }
    
    public static class WorkerChangeEvent {
        private Set<String> addresses;

        public WorkerChangeEvent(Set<String> addresses) {
            this.addresses = addresses;
        }

        public Set<String> getAddresses() {
            return addresses;
        }

        public void setAddresses(Set<String> addresses) {
            this.addresses = addresses;
        }
    }
}
