package com.hotcaffeine.common.etcd;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.hotcaffeine.common.etcd.EtcdClient.KV;

import io.etcd.jetcd.Watch.Listener;
import io.etcd.jetcd.auth.Permission.Type;
import io.etcd.jetcd.watch.WatchResponse;

public class EtcdClientTest {

    private String root = "root";
    private String password = "123456";
    private String userApi = "api";
    private String apiPassword = "api123456";
    
    private String clientRole = "client";
    
    private String rootResource = "/hotcaffeine/";
    
    EtcdClient etcdClient = new EtcdClient("http://127.0.0.1:2379", root, password);
    
    @Test
    public void testPut() {
        String key = "/hotcaffeine/test/3";
        String value = "哈哈哈";
        etcdClient.put(key, value);
        etcdClient.close();
    }
    
    @Test
    public void testGet() {
        String key = "/hotcaffeine/test";
        String value = etcdClient.get(key);
        Assert.assertNotNull(value);
        etcdClient.close();
    }

    @Test
    public void testDelete() {
        String key = "/hotcaffeine/test";
        etcdClient.delete(key);
        etcdClient.close();
    }
    
    @Test
    public void testPutTtl() {
        String key = "/hotcaffeine/test";
        String value = "哈哈哈";
        int ttl = 300;
        etcdClient.put(key, value, ttl);
        etcdClient.close();
    }
    
    @Test
    public void testGetList() {
        String key = "/hotcaffeine/test/1";
        int ttl = 10;
        String value = "哈哈哈";
        etcdClient.put(key, value, ttl);
        key = "/hotcaffeine/test/2";
        value = "哈哈哈2";
        etcdClient.put(key, value, ttl);
        key = "/hotcaffeine/test";
        List<KV> list = etcdClient.getPrefix(key);
        Assert.assertEquals(2, list.size());
        etcdClient.close();
    }
    
    @Test
    public void testWatch() throws InterruptedException {
        String key = "/hotcaffeine/test/1";
        int ttl = 10;
        String value = "哈哈哈";
        etcdClient.put(key, value, ttl);
        etcdClient.watch(key, new Listener() {
            @Override
            public void onNext(WatchResponse response) {
                response.getEvents().forEach(e->{
                    System.out.println(e.getEventType()+","+etcdClient.toKV(e.getKeyValue()));
                });
            }
            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
            }
            @Override
            public void onCompleted() {
                System.out.println("Completed");
            }
        });
        Thread.sleep(30 * 1000);
        etcdClient.close();
    }
    
    @Test
    public void testWatchPrefix() throws InterruptedException {
        String key = "/hotcaffeine/test";
        etcdClient.watchPrefix(key, new Listener() {
            @Override
            public void onNext(WatchResponse response) {
                response.getEvents().forEach(e->{
                    System.out.println(e.getEventType()+","+etcdClient.toKV(e.getKeyValue()));
                });
            }
            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
            }
            @Override
            public void onCompleted() {
                System.out.println("Completed");
            }
        });
        Thread.sleep(10 * 60 * 1000);
        etcdClient.close();
    }
    
    @Test
    public void testRootAdd() {
        etcdClient.userAdd(root, password);
    }
    
    @Test
    public void testRootRoleAdd() {
        etcdClient.userGrantRole(root, root);
    }
    
    @Test
    public void testAuthEnable() {
        etcdClient.authEnable();
    }
    
    @Test
    public void testRootOption() {
        etcdClient = new EtcdClient("http://127.0.0.1:2379", root, password);
        String key = "/key";
        String value = "abc123";
        etcdClient.put(key, value);
        Assert.assertEquals(value, etcdClient.get(key));
        etcdClient.delete(key);
        Assert.assertEquals(null, etcdClient.get(key));
    }
    
    @Test
    public void testRoleAdd() {
        etcdClient = new EtcdClient("http://127.0.0.1:2379", root, password);
        etcdClient.roleAdd(clientRole);
    }
    
    @Test
    public void testRoleGrantPermission() {
        etcdClient = new EtcdClient("http://127.0.0.1:2379", root, password);
        etcdClient.roleGrantPermission(clientRole, rootResource, true, Type.READ);
    }
    
    @Test
    public void userGrantRole() {
        etcdClient = new EtcdClient("http://127.0.0.1:2379", root, password);
        etcdClient.userGrantRole(userApi, clientRole);
    }
    
    @Test
    public void testUserOption() throws InterruptedException {
        etcdClient = new EtcdClient("http://127.0.0.1:2379", userApi, apiPassword);
        String key = "/hotcaffeine/test";
        Assert.assertEquals("哈哈哈", etcdClient.get(key));
        Assert.assertEquals(2, etcdClient.getPrefix(key).size());
        etcdClient.watchPrefix(key, new Listener() {
            public void onNext(WatchResponse response) {
                System.out.println("onNext:"+response.getEvents().size());
                System.out.println("onNext:"+response.getEvents().get(0).getEventType());
            }
            public void onError(Throwable throwable) {
                System.out.println("="+throwable.toString());
            }
            public void onCompleted() {
                System.out.println("onCompleted");
            }
        });
        System.out.println("testUserOption");
        Thread.sleep(10 * 60 * 1000);
    }
}
