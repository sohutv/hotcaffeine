package com.hotcaffeine.dashboard.view.data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.hotcaffeine.data.TopHotKeyStore;
import com.hotcaffeine.data.metric.TopHotKey;

/**
 * topk调用量
 * 
 * @author yongfeigao
 * @date 2021年3月5日
 */
@Component
public class TopkCountLineChartData implements LineChartData {

    @Autowired
    private TopHotKeyStore topHotKeyStore;

    // 搜索区域
    private SearchHeader searchHeader;

    public static final String APPNAME_FIELD = "appName";
    public static final String RULEKEY_FIELD = "ruleKey";
    public static final String TIME_FIELD = "time";

    public TopkCountLineChartData() {
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
        return "topkCount";
    }

    @Override
    public String getPageTitle() {
        return "topk调用量";
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
        List<TopHotKey> topHotKeyList = topHotKeyStore.queryTopHotKey(appName, ruleKey, date.getTime());

        List<LineChart> lineChartList = new ArrayList<LineChart>();
        if (topHotKeyList == null || topHotKeyList.size() == 0) {
            return lineChartList;
        }
        Collections.sort(topHotKeyList, (o1, o2)->{
            return o1.getMinute().compareTo(o2.getMinute());
        });

        // 构造曲线图对象
        LineChart lineChart = new LineChart();
        lineChartList.add(lineChart);
        lineChart.setChartId("topkCount");
        lineChart.setTitle("topk调用量");
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
        distributionYAxisGroup.setGroupName("topk调用量/总调用量");
        distributionYAxisGroup.setFormatSuffix("%");
        distributionYAxisGroup.setyAxisList(distributionYAxisList);
        // 设置y轴
        List<YAxisGroup> yAxisGroupList = new ArrayList<YAxisGroup>();
        yAxisGroupList.add(distributionYAxisGroup);
        lineChart.setyAxisGroupList(yAxisGroupList);

        Map<Integer, YAxis> yAxisMap = buildYAxisMap(topHotKeyList);
        for (TopHotKey topHotKey : topHotKeyList) {
            xList.add(topHotKey.getMinute());
            // 填充y轴数据
            for (Integer couner : yAxisMap.keySet()) {
                YAxis yAxis = yAxisMap.get(couner);
                // 填充y轴数据
                Map<String, Object> countMap = new HashMap<String, Object>();
                if(topHotKey.getTopkSize() != couner) {
                    countMap.put("y", 0);
                } else {
                    countMap.put("y", topHotKey.topCountRate());
                    countMap.put("c", topHotKey.getTopCount());
                    countMap.put("tc", topHotKey.getTotalCount());
                }
                yAxis.getData().add(countMap);
            }
        }
        distributionYAxisList.addAll(yAxisMap.values());
        return lineChartList;
    }
    
    private Map<Integer, YAxis> buildYAxisMap(List<TopHotKey> topHotKeyList) {
        Map<Integer, YAxis> yAxisMap = new HashMap<Integer, LineChart.YAxis>();
        for (TopHotKey topHotKey : topHotKeyList) {
            yAxisMap.computeIfAbsent(topHotKey.getTopkSize(), k -> {
                YAxis yAxis = new YAxis();
                yAxis.setName("top" + k);
                yAxis.setData(new ArrayList<>());
                return yAxis;
            });
        }
        return yAxisMap;
    }
}
