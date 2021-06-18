package com.hotcaffeine.common.etcd;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.hotcaffeine.common.util.ClientLogger;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.ClientBuilder;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.Watch.Listener;
import io.etcd.jetcd.Watch.Watcher;
import io.etcd.jetcd.auth.AuthEnableResponse;
import io.etcd.jetcd.auth.AuthRoleAddResponse;
import io.etcd.jetcd.auth.AuthRoleGrantPermissionResponse;
import io.etcd.jetcd.auth.AuthUserAddResponse;
import io.etcd.jetcd.auth.AuthUserChangePasswordResponse;
import io.etcd.jetcd.auth.AuthUserGetResponse;
import io.etcd.jetcd.auth.AuthUserGrantRoleResponse;
import io.etcd.jetcd.auth.AuthUserListResponse;
import io.etcd.jetcd.auth.Permission.Type;
import io.etcd.jetcd.kv.DeleteResponse;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.kv.PutResponse;
import io.etcd.jetcd.lease.LeaseGrantResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.shaded.io.netty.util.internal.StringUtil;
import io.etcd.jetcd.watch.WatchEvent;
import io.etcd.jetcd.watch.WatchResponse;

/**
 * etcd客户端
 * 
 * @author yongfeigao
 * @date 2021年1月22日
 */
public class EtcdClient {
    // 端点
    private String endpoints;

    // 客户端
    private Client client;
    
    // watcher列表，供关闭使用
    private List<Watcher> watcherList = new ArrayList<>();

    public EtcdClient(String endpoints, String user, String password) {
        this.endpoints = endpoints;
        ClientBuilder builder = Client.builder().endpoints(endpoints.split(","));
        if (!StringUtil.isNullOrEmpty(user) && !StringUtil.isNullOrEmpty(password)) {
            builder.user(ByteSequence.from(user.getBytes())).password(ByteSequence.from(password.getBytes()));
        }
        client = builder.build();
        ClientLogger.getLogger().info("init user:{} endpoints:{}", user, endpoints);
    }

    /**
     * 存入
     * 
     * @param key
     * @param value
     * @return PutResponse
     */
    public PutResponse put(String key, String value) {
        return put(key, value, -1);
    }

    /**
     * 存入，带超时
     * 
     * @param key
     * @param value
     * @param ttl
     * @return PutResponse
     */
    public PutResponse put(String key, String value, long ttl) {
        ByteSequence k = ByteSequence.from(key.getBytes());
        ByteSequence v = ByteSequence.from(value.getBytes());
        try {
            if (ttl < 0) {
                return client.getKVClient().put(k, v).get();
            } else {
                LeaseGrantResponse leaseGrantResponse = client.getLeaseClient().grant(ttl).get();
                PutOption putOption = PutOption.newBuilder().withLeaseId(leaseGrantResponse.getID()).build();
                return client.getKVClient().put(k, v, putOption).get();
            }
        } catch (Exception e) {
            ClientLogger.getLogger().error("put key:{} value:{} error", key, value, e);
            throw new EtcdException(e);
        }
    }

    /**
     * 删除
     * 
     * @param key
     * @return DeleteResponse
     */
    public DeleteResponse delete(String key) {
        try {
            ByteSequence k = ByteSequence.from(key.getBytes());
            return client.getKVClient().delete(k).get();
        } catch (Exception e) {
            ClientLogger.getLogger().error("delete key:{} error", key, e);
            throw new EtcdException(e);
        }
    }

    /**
     * 获取单个value
     * 
     * @param key
     * @return String
     */
    public String get(String key) {
        KV kv = getKV(key);
        if (kv == null) {
            return null;
        }
        return kv.getValue();
    }

    /**
     * 获取单个value
     * 
     * @param key
     * @return String
     */
    public KV getKV(String key) {
        List<KV> list = getList(key);
        if (list == null || list.size() <= 0) {
            return null;
        }
        return list.get(0);
    }

    /**
     * 获取多个value
     * 
     * @param key
     * @return List<KeyValue>
     */
    public List<KV> getList(String key) {
        return toKVList(getList(key, false));
    }

    /**
     * 获取前缀
     * 
     * @param key
     * @return
     */
    public List<KV> getPrefix(String key) {
        return toKVList(getList(key, true));
    }

    public List<KeyValue> getList(String key, boolean prefix) {
        ByteSequence k = ByteSequence.from(key.getBytes());
        GetOption getOption = null;
        if (prefix) {
            getOption = GetOption.newBuilder().withPrefix(k).build();
        }
        return getList(k, getOption);
    }

    public List<KeyValue> getList(ByteSequence key, GetOption getOption) {
        try {
            GetResponse getResponse = null;
            if (getOption != null) {
                getResponse = client.getKVClient().get(key, getOption).get();
            } else {
                getResponse = client.getKVClient().get(key).get();
            }
            return getResponse.getKvs();
        } catch (Exception e) {
            ClientLogger.getLogger().error("getList key:{} error", key.toString(Charset.defaultCharset()), e);
            throw new EtcdException(e);
        }
    }

    /**
     * 监听
     * 
     * @param key
     * @param listener
     * @return
     */
    public Watcher watch(String key, Listener listener) {
        return watch(key, false, listener);
    }

    /**
     * 监听前缀
     * 
     * @param key
     * @param listener
     * @return
     */
    public Watcher watchPrefix(String key, Listener listener) {
        return watch(key, true, listener);
    }

    public Watcher watch(String key, boolean prefix, Listener listener) {
        ByteSequence k = ByteSequence.from(key.getBytes());
        WatchOption option = null;
        if (prefix) {
            option = WatchOption.newBuilder().withPrefix(k).build();
        }
        return watch(k, option, listener);
    }

    public Watcher watch(ByteSequence key, WatchOption watchOption, Listener listener) {
        try {
            if (watchOption != null) {
                return client.getWatchClient().watch(key, watchOption, listener);
            } else {
                return client.getWatchClient().watch(key, listener);
            }
        } catch (Exception e) {
            ClientLogger.getLogger().error("watch key:{} error", key.toString(Charset.defaultCharset()), e);
            throw new EtcdException(e);
        }
    }

    private List<KV> toKVList(List<KeyValue> list) {
        if (list == null) {
            return null;
        }
        if (list.size() == 0) {
            return new ArrayList<>(0);
        }
        List<KV> kvList = new ArrayList<>(list.size());
        list.forEach(kv -> {
            kvList.add(toKV(kv));
        });
        return kvList;
    }

    public KV toKV(WatchEvent watchEvent) {
        KV kv = toKV(watchEvent.getKeyValue());
        kv.setEventType(EventType.getEventType(watchEvent.getEventType().ordinal()));
        return kv;
    }

    public KV toKV(KeyValue kv) {
        String key = kv.getKey().toString(Charset.defaultCharset());
        String value = null;
        if (kv.getValue() != null) {
            value = kv.getValue().toString(Charset.defaultCharset());
        }
        return new KV(key, value, kv.getModRevision());
    }

    /**
     * 监听
     * 
     * @param watchPath
     * @param consumer
     */
    public void watch(String watchPath, Consumer<KV> consumer) {
        ClientLogger.getLogger().info("watch prefix:{}", watchPath);
        watcherList.add(watchPrefix(watchPath, new Listener() {
            public void onNext(WatchResponse response) {
                List<WatchEvent> eventList = response.getEvents();
                if (eventList.size() == 0) {
                    return;
                }
                KV kv = toKV(eventList.get(0));
                ClientLogger.getLogger().info("{} changed:{}", watchPath, kv);
                try {
                    consumer.accept(kv);
                } catch (Throwable e) {
                    ClientLogger.getLogger().error("watch:{} error", watchPath, e);
                }
            }

            public void onError(Throwable throwable) {
                ClientLogger.getLogger().error("watch:{} error", watchPath, throwable);
            }

            public void onCompleted() {
                ClientLogger.getLogger().info("watch:{} completed", watchPath);
            }
        }));
    }

    public void close() {
        watcherList.forEach(w -> {
            w.close();
        });
        client.close();
        ClientLogger.getLogger().info("etcd client shutdown");
    }

    public String getEndpoints() {
        return endpoints;
    }

    // following is auth api

    /**
     * 权限开启
     * 
     * @return
     */
    public AuthEnableResponse authEnable() {
        try {
            return client.getAuthClient().authEnable().get();
        } catch (Exception e) {
            ClientLogger.getLogger().error("authEnable error", e);
            throw new EtcdException(e);
        }
    }

    /**
     * 添加用户
     * 
     * @param user
     * @param password
     * @return
     */
    public AuthUserAddResponse userAdd(String user, String password) {
        try {
            ByteSequence u = ByteSequence.from(user.getBytes());
            ByteSequence p = ByteSequence.from(password.getBytes());
            return client.getAuthClient().userAdd(u, p).get();
        } catch (Exception e) {
            ClientLogger.getLogger().error("userAdd error:{}", user, e);
            throw new EtcdException(e);
        }
    }
    
    /**
     * 获取用户
     * 
     * @param user
     * @return
     */
    public AuthUserGetResponse userGet(String user) {
        try {
            ByteSequence u = ByteSequence.from(user.getBytes());
            return client.getAuthClient().userGet(u).get();
        } catch (Exception e) {
            ClientLogger.getLogger().error("userGet error:{}", user, e);
            throw new EtcdException(e);
        }
    }

    /**
     * 修改密码
     * 
     * @param user
     * @param password
     * @return
     */
    public AuthUserChangePasswordResponse userChangePassword(String user, String password) {
        try {
            ByteSequence u = ByteSequence.from(user.getBytes());
            ByteSequence p = ByteSequence.from(password.getBytes());
            return client.getAuthClient().userChangePassword(u, p).get();
        } catch (Exception e) {
            ClientLogger.getLogger().error("userChangePassword error:{} password:{}", user, password, e);
            throw new EtcdException(e);
        }
    }

    /**
     * 用户列表
     * 
     * @return
     */
    public AuthUserListResponse userList() {
        try {
            return client.getAuthClient().userList().get();
        } catch (Exception e) {
            ClientLogger.getLogger().error("userList error", e);
            throw new EtcdException(e);
        }
    }

    /**
     * 添加角色
     * 
     * @param role
     * @return
     */
    public AuthRoleAddResponse roleAdd(String role) {
        try {
            ByteSequence r = ByteSequence.from(role.getBytes());
            return client.getAuthClient().roleAdd(r).get();
        } catch (Exception e) {
            ClientLogger.getLogger().error("roleAdd:{} error", role, e);
            throw new EtcdException(e);
        }
    }

    /**
     * 用户分配角色
     * 
     * @param user
     * @param role
     * @return
     */
    public AuthUserGrantRoleResponse userGrantRole(String user, String role) {
        try {
            ByteSequence u = ByteSequence.from(user.getBytes());
            ByteSequence r = ByteSequence.from(role.getBytes());
            return client.getAuthClient().userGrantRole(u, r).get();
        } catch (Exception e) {
            ClientLogger.getLogger().error("userGrantRole user:{} role:{} error", user, role, e);
            throw new EtcdException(e);
        }
    }

    /**
     * 角色分配权限
     * 
     * @param role
     * @return
     */
    public AuthRoleGrantPermissionResponse roleGrantPermission(String role, String key, Type perm) {
        return roleGrantPermission(role, key, true, perm);
    }

    /**
     * 角色分配权限
     * 
     * @param role
     * @return
     */
    public AuthRoleGrantPermissionResponse roleGrantPermission(String role, String key, boolean prefix, Type perm) {
        try {
            ByteSequence r = ByteSequence.from(role.getBytes());
            ByteSequence k = ByteSequence.from(key.getBytes());
            ByteSequence end = k;
            if (prefix) {
                Optional<ByteSequence> optional = GetOption.newBuilder().withPrefix(k).build().getEndKey();
                if (optional.isPresent()) {
                    end = optional.get();
                }
            }
            return client.getAuthClient().roleGrantPermission(r, k, end, perm).get();
        } catch (Exception e) {
            ClientLogger.getLogger().error("roleGrantPermission role:{} key:{} prefix:{} perm:{} error", role, key, prefix, perm, e);
            throw new EtcdException(e);
        }
    }

    /**
     * kv
     * 
     * @author yongfeigao
     * @date 2021年1月22日
     */
    public static class KV {
        private String key;
        private String value;
        private long modRevision;
        private EventType eventType;

        public KV(String key, String value, long modRevision) {
            this.key = key;
            this.value = value;
            this.modRevision = modRevision;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public long getModRevision() {
            return modRevision;
        }

        public void setModRevision(long modRevision) {
            this.modRevision = modRevision;
        }

        public EventType getEventType() {
            return eventType;
        }

        public void setEventType(EventType eventType) {
            this.eventType = eventType;
        }

        @Override
        public String toString() {
            return "KV [key=" + key + ", value=" + value + ", modRevision=" + modRevision + ", eventType=" + eventType
                    + "]";
        }
    }
    
    public enum EventType {
        PUT, DELETE, UNRECOGNIZED,
        ;
        public static EventType getEventType(int ordinal) {
            if(PUT.ordinal() == ordinal) {
                return PUT;
            }
            if(DELETE.ordinal() == ordinal) {
                return DELETE;
            }
            return UNRECOGNIZED;
        }
    }

    /**
     * 异常
     * 
     * @author yongfeigao
     * @date 2021年1月22日
     */
    public static class EtcdException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public EtcdException() {
            super();
        }

        public EtcdException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }

        public EtcdException(String message, Throwable cause) {
            super(message, cause);
        }

        public EtcdException(String message) {
            super(message);
        }

        public EtcdException(Throwable cause) {
            super(cause);
        }
    }
}
