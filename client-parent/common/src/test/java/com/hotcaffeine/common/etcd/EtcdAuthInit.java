package com.hotcaffeine.common.etcd;

import io.etcd.jetcd.auth.AuthUserGetResponse;
import io.etcd.jetcd.auth.AuthUserListResponse;
import io.etcd.jetcd.auth.Permission.Type;

public class EtcdAuthInit {
    public static void main(String[] args) {
        String endpoints = "http://127.0.0.1:2379";
        EtcdClient etcdClient = new EtcdClient(endpoints, null, null);
        String root = "root";
        String password = "H&O%T=C9AFF]";

        // 1.初始化root用户及密码
        etcdClient.userAdd(root, password);
        AuthUserListResponse authUserListResponse = etcdClient.userList();
        if (authUserListResponse.getUsers().stream().anyMatch(u -> root.equals(u))) {
            System.out.println("user root init ok");
        } else {
            System.err.println("root init err! users:" + authUserListResponse.getUsers());
            return;
        }

        // 2.root用户赋予root角色
        etcdClient.userGrantRole(root, root);
        AuthUserGetResponse authUserGetResponse = etcdClient.userGet(root);
        if (authUserGetResponse.getRoles().stream().anyMatch(u -> root.equals(u))) {
            System.out.println("root grant role ok");
        } else {
            System.err.println("root grant role! roles:" + authUserGetResponse.getRoles());
            return;
        }
        
        // 3.开启认证
        etcdClient.authEnable();
        
        // 4.添加worker
        String userWorker = "worker";
        etcdClient = new EtcdClient(endpoints, root, password);
        etcdClient.userAdd(userWorker, "=+WK%&#*");
        String workerRole = "worker";
        DefaultEtcdConfig defaultEtcdConfig = new DefaultEtcdConfig();
        etcdClient.roleAdd(workerRole);
        etcdClient.roleGrantPermission(workerRole, defaultEtcdConfig.getRootPath(), Type.READWRITE);
        etcdClient.userGrantRole(userWorker, workerRole);
        AuthUserGetResponse workerUserGetResponse = etcdClient.userGet(userWorker);
        if (workerUserGetResponse.getRoles().stream().anyMatch(u -> userWorker.equals(u))) {
            System.out.println("worker grant role ok");
        } else {
            System.err.println("worker grant role! roles:" + workerUserGetResponse.getRoles());
            return;
        }
        
        // 5.添加client
        String clientRole = "client";
        etcdClient.roleAdd(clientRole);
        etcdClient.roleGrantPermission(clientRole, defaultEtcdConfig.getRootPath(), Type.READ);
        System.out.println("finish");
        System.exit(0);
    }
}
