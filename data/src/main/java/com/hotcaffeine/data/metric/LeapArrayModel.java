package com.hotcaffeine.data.metric;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
/**
 * 滑动窗口模型
 * 
 * @author yongfeigao
 * @date 2021年3月12日
 */
public class LeapArrayModel {
    private int windowLengthInMs;
    private int sampleCount;
    private int intervalInMs;
    private int liveTime;
    private int survivalTime;
    private long totalCount;

    private List<WindowWrapModel> windowList;

    public int getWindowLengthInMs() {
        return windowLengthInMs;
    }

    public void setWindowLengthInMs(int windowLengthInMs) {
        this.windowLengthInMs = windowLengthInMs;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(int sampleCount) {
        this.sampleCount = sampleCount;
    }

    public int getIntervalInMs() {
        return intervalInMs;
    }

    public void setIntervalInMs(int intervalInMs) {
        this.intervalInMs = intervalInMs;
    }

    public int getLiveTime() {
        return liveTime;
    }

    public void setLiveTime(int liveTime) {
        this.liveTime = liveTime;
    }

    public int getSurvivalTime() {
        return survivalTime;
    }

    public void setSurvivalTime(int survivalTime) {
        this.survivalTime = survivalTime;
    }

    public List<WindowWrapModel> getWindowList() {
        return windowList;
    }

    public void setWindowList(List<WindowWrapModel> windowList) {
        this.windowList = windowList;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }
    
    public long getValidCount() {
        if (windowList == null || windowList.size() == 0) {
            return 0;
        }
        long count = 0;
        for (WindowWrapModel w : windowList) {
            if (!w.isDeprecated()) {
                count += w.getValue();
            }
        }
        return count;
    }

    public static class WindowWrapModel implements Comparable<WindowWrapModel>{
        /**
         * Start timestamp of the window in milliseconds.
         */
        private long windowStart;

        /**
         * Statistic data.
         */
        private long value;
        
        private boolean deprecated;
        
        private boolean counting;
        
        public long getWindowStart() {
            return windowStart;
        }

        public void setWindowStart(long windowStart) {
            this.windowStart = windowStart;
        }
        
        public String getWindowStartFormated() {
            return new SimpleDateFormat("HH:mm:ss.SSS").format(new Date(windowStart));
        }
        
        public boolean isCounting() {
            return counting;
        }

        public void setCounting(boolean counting) {
            this.counting = counting;
        }

        public long getValue() {
            return value;
        }

        public void setValue(long value) {
            this.value = value;
        }

        public boolean isDeprecated() {
            return deprecated;
        }

        public void setDeprecated(boolean deprecated) {
            this.deprecated = deprecated;
        }

        @Override
        public int compareTo(WindowWrapModel o) {
            return (int) (windowStart - o.windowStart);
        }
    }

}
