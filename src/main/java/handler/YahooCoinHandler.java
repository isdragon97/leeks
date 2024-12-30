package handler;

import bean.CoinBean;
import bean.YahooResponse;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.google.common.base.Joiner;
import com.google.gson.Gson;
import utils.HttpClientPool;
import utils.LogUtil;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class YahooCoinHandler extends CoinRefreshHandler {
    private final String URL = "https://query1.finance.yahoo.com/v7/finance/quote?&symbols=";
    private final String URL1 = "https://eea.okx.com/api/v5/public/price-limit?instId=";
    private final String URL2 = "https://eea.okx.com/api/v5/public/mark-price?instId=";
    private static final String URL3 = "https://eea.okx.com/api/v5/public/price-limit?instId=";
    private final String KEYS = "&fields=regularMarketChange,regularMarketChangePercent,regularMarketPrice,regularMarketTime,regularMarketDayHigh,regularMarketDayLow";
    private final JLabel refreshTimeLabel;

    private final Gson gson = new Gson();

    public YahooCoinHandler(JTable table, JLabel label) {
        super(table);
        this.refreshTimeLabel = label;
    }

    @Override
    public void handle(List<String> code) {
        if (code.isEmpty()) {
            return;
        }

        pollStock(code);
    }

    private void pollStock(List<String> code) {

        if (code.isEmpty()){
            return;
        }
//        String params = Joiner.on(",").join(code);
        for (String s : code) {
            try {
                String res = HttpClientPool.getHttpClient().get(URL1 + s);
                String res2 = HttpClientPool.getHttpClient().get(URL2 + s);
              handleResponse(res,res2);
            } catch (Exception e) {
                LogUtil.info(e.getMessage());
            }
        }

    }

    public void handleResponse(String response,String response2) {
//        System.out.println("解析虚拟币："+response);
        List<String> refreshTimeList = new ArrayList<>();
        try{
//            YahooResponse yahooResponse = gson.fromJson(response, YahooResponse.class);
//            for (CoinBean coinBean : yahooResponse.getQuoteResponse().getResult()) {
            Map map1 = gson.fromJson(response, Map.class);
            String list = Convert.toStr(map1.get("data"));
            List list1 =  gson.fromJson(list, List.class);
            Map map2 = gson.fromJson(response2, Map.class);
            String list2 = Convert.toStr(map2.get("data"));
            List list3 =  gson.fromJson(list2, List.class);
            if (list1.size() > 0){
//            for (Object o : list1) {
                Object o = list1.get(0);
                Map<String, Object> map = Convert.toMap(String.class, Object.class, o);
                Double sellLmt = Convert.toDouble(map.get("sellLmt"));
                Double buyLmt = Convert.toDouble(map.get("buyLmt"));
                String instId = Convert.toStr(map.get("instId"));
                CoinBean coinBean = new  CoinBean(instId);
                coinBean.setRegularMarketDayHigh(buyLmt);
                coinBean.setRegularMarketDayLow(sellLmt);
                coinBean.setRegularMarketDayLow(sellLmt);
                coinBean.setRegularMarketChange(0);
                coinBean.setRegularMarketChangePercent(0);
                coinBean.setRegularMarketTime(Convert.toLong(map.get("ts")));
                if (list3.size() > 0){
                    Map<String, Object> map5 = Convert.toMap(String.class, Object.class, list3.get(0));
                    if (Objects.nonNull(map5.get("markPx")) && StrUtil.isNotEmpty(Convert.toStr(map5.get("markPx")))){
                        coinBean.setRegularMarketPrice(Convert.toDouble(map5.get("markPx")));
                    }else {
                        coinBean.setRegularMarketPrice(sellLmt);
                    }
                }else {
                    coinBean.setRegularMarketPrice(sellLmt);
                }

                updateData(coinBean);
                refreshTimeList.add(coinBean.getValueByColumn("更新时间",false));
            }
            
//            }
        }catch (Exception e){
            System.out.println(e.toString());
        }

        String text = refreshTimeList.stream().sorted().findFirst().orElse("");
        SwingUtilities.invokeLater(() -> refreshTimeLabel.setText(text));
    }

    @Override
    public void stopHandle() {
        LogUtil.info("leeks stock 自动刷新关闭!");
    }
}
