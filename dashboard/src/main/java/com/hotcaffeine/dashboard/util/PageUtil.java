package com.hotcaffeine.dashboard.util;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.CollectionUtils;

import com.hotcaffeine.dashboard.common.domain.Page;

import io.etcd.jetcd.shaded.com.google.common.collect.Lists;

/**
 * @author wuweifeng
 * @version 1.0
 * @date 2020-09-01
 */
public class PageUtil {
    /**
     * 通用分页工具类
     *
     * @param data
     * @param pageSize
     * @param pageNum
     * @param <T>
     * @return
     */
    public static <T> Page<T> pagination(final List<T> data, final int pageSize, final int pageNum) {
        if (CollectionUtils.isEmpty(data)) {
            return new Page<>(1, 0, new ArrayList<>());
        }
        List<List<T>> lists = Lists.partition(data, pageSize);
        int localPageNum = pageNum;
        if (localPageNum >= lists.size()) {
            localPageNum = lists.size() - 1;
        }
        return new Page<>(localPageNum, data.size(), lists.get(localPageNum));
    }
}
