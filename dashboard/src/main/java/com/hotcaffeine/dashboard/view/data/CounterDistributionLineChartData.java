package com.hotcaffeine.dashboard.view.data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hotcaffeine.dashboard.view.SearchHeader;
import com.hotcaffeine.dashboard.view.SearchHeader.HiddenSearchField;
import com.hotcaffeine.dashboard.view.SearchHeader.SearchField;
import com.hotcaffeine.dashboard.view.SearchHeader.SelectSearchField;
import com.hotcaffeine.dashboard.view.chart.LineChart;
import com.hotcaffeine.dashboard.view.chart.LineChart.XAxis;
import com.hotcaffeine.dashboard.view.chart.LineChart.YAxis;
import com.hotcaffeine.dashboard.view.chart.LineChart.YAxisGroup;
import com.hotcaffeine.dashboard.view.chart.LineChartData;
import com.hotcaffeine.data.CounterDistributionStore;
import com.hotcaffeine.data.metric.KeyCounter;
import com.hotcaffeine.data.metric.KeyCounterData;

/**
 * 调用量分布
 * 
 * @author yongfeigao
 * @date 2021年3月5日
 */
@Component
public class CounterDistributionLineChartData implements LineChartData {

    @Autowired
    private CounterDistributionStore counterDistributionStore;

    // 搜索区域
    private SearchHeader searchHeader;

    public static final String APPNAME_FIELD = "appName";
    public static final String RULEKEY_FIELD = "ruleKey";
    public static final String TIME_FIELD = "time";

    public CounterDistributionLineChartData() {
        initSearchHeader();
    }

    /**
     * 初始化搜索数据
     */
    public void initSearchHeader() {
        searchHeader = new SearchHeader();
        List<SearchField> searchFieldList = new ArrayList<SearchHeader.SearchField>();

        // appName
        SelectSearchField appNameSearchField = new SelectSearchField();
        appNameSearchField.setKey(APPNAME_FIELD);
        searchFieldList.add(appNameSearchField);

        // ruleKey
        HiddenSearchField ruleKeySearchField = new HiddenSearchField();
        ruleKeySearchField.setKey(RULEKEY_FIELD);
        searchFieldList.add(ruleKeySearchField);

        // time
        HiddenSearchField dateSearchField = new HiddenSearchField();
        dateSearchField.setKey(TIME_FIELD);
        searchFieldList.add(dateSearchField);

        searchHeader.setSearchFieldList(searchFieldList);
    }

    @Override
    public String getPath() {
        return "counterDistribution";
    }

    @Override
    public String getPageTitle() {
        return "调用量分布";
    }

    @Override
    public SearchHeader getSearchHeader() {
        return searchHeader;
    }

    @Override
    public List<LineChart> getLineChartData(Map<String, Object> searchMap) {
        String appName = (String) searchMap.get(APPNAME_FIELD);
        String ruleKey = (String) searchMap.get(RULEKEY_FIELD);
        Date date = new Date();
        try {
            date = new SimpleDateFormat("yyyy-MM-dd HH").parse(searchMap.get(TIME_FIELD).toString());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        List<KeyCounterData> keyCounterDataList = counterDistributionStore.query(appName, ruleKey, date.getTime());

        List<LineChart> lineChartList = new ArrayList<LineChart>();
        if (keyCounterDataList == null || keyCounterDataList.size() == 0) {
            return lineChartList;
        }
        // 合并数据
        Map<String, KeyCounterData> keyCounterDataMap = merge(keyCounterDataList);
        
        // 构造曲线图对象
        LineChart lineChart = new LineChart();
        lineChartList.add(lineChart);
        lineChart.setChartId("counterDistribution");
        lineChart.setTitle("调用量分布");
        lineChart.setOneline(true);
        lineChart.setBorderWidth(0);

        // 构造x轴
        List<String> xList = new ArrayList<>();
        XAxis xAxis = new XAxis();
        xAxis.setxList(xList);
        lineChart.setxAxis(xAxis);

        // 设置y轴列表
        List<YAxis> distributionYAxisList = new ArrayList<YAxis>();
        // 生成y轴数据组
        YAxisGroup distributionYAxisGroup = new YAxisGroup();
        distributionYAxisGroup.setGroupName("key数量");
        distributionYAxisGroup.setyAxisList(distributionYAxisList);
        // 设置y轴
        List<YAxisGroup> yAxisGroupList = new ArrayList<YAxisGroup>();
        yAxisGroupList.add(distributionYAxisGroup);
        lineChart.setyAxisGroupList(yAxisGroupList);

        Map<Integer, YAxis> yAxisMap = buildYAxisMap(keyCounterDataMap);
        for (String tm : keyCounterDataMap.keySet()) {
            xList.add(tm);
            KeyCounterData keyCounterData = keyCounterDataMap.get(tm);

            // 填充y轴数据
            for (Integer couner : yAxisMap.keySet()) {
                YAxis yAxis = yAxisMap.get(couner);
                Map<String, Object> countMap = new HashMap<String, Object>();
                KeyCounter keyCounter = keyCounterData.getDistributionMap().get(couner);
                if(keyCounter == null) {
                    countMap.put("y", 0);
                } else {
                    countMap.put("y", keyCounter.getKeyCount());
                    countMap.put("r", keyCounter.keyCountRate() * 100);
                    countMap.put("t", KeyCounter.toSecond(keyCounter.getSurvivalTime() / keyCounter.getKeyCount()));
                    countMap.put("mxt", KeyCounter.toSecond(keyCounter.getMaxSurvivalTime()));
                    countMap.put("mit", KeyCounter.toSecond(keyCounter.getMinSurvivalTime()));
                }
                yAxis.getData().add(countMap);
            }
        }
        distributionYAxisList.addAll(yAxisMap.values());

        return lineChartList;
    }

    private Map<Integer, YAxis> buildYAxisMap(Map<String, KeyCounterData> keyCounterDataMap) {
        Map<Integer, YAxis> yAxisMap = new HashMap<Integer, LineChart.YAxis>();
        for (KeyCounterData keyCounterData : keyCounterDataMap.values()) {
            for (Integer count : keyCounterData.getDistributionMap().keySet()) {
                yAxisMap.computeIfAbsent(count, k -> {
                    YAxis yAxis = new YAxis();
                    yAxis.setName("调用量=" + k);
                    yAxis.setData(new ArrayList<>());
                    return yAxis;
                });
            }
        }
        return yAxisMap;
    }

    private Map<String, KeyCounterData> merge(List<KeyCounterData> keyCounterDataList) {
        Map<String, KeyCounterData> map = new TreeMap<>();
        keyCounterDataList.forEach(keyCounterData -> {
            map.computeIfAbsent(keyCounterData.getMinute(), k -> {
                return new KeyCounterData(k);
            }).add(keyCounterData);
        });
        map.forEach((k, v) -> {
            v.initKeyCountRate();
        });
        return map;
    }
}
