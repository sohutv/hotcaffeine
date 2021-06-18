package com.hotcaffeine.common.model;

/**
 * 是否可销毁
 * @Description: 
 * @author yongfeigao
 * @date 2018年3月5日
 */
public interface Destroyable extends Comparable<Destroyable>{
    /**
     * 销毁
     * @throws Exception
     */
    public void destroy() throws Exception;
    
    /**
     * 销毁的顺序，顺序越小，越靠前销毁
     * @return
     */
    public int order();
    
    /**
     * 比较order
     */
    default public int compareTo(Destroyable o) {
        return order() - o.order();
    }
}
