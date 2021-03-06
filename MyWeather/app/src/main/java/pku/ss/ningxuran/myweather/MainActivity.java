package pku.ss.ningxuran.myweather;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.logging.LogRecord;

import pku.ss.ningxuran.bean.TodayWeather;
import pku.ss.ningxuran.util.NetUtil;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageView mUpdateBtn,mCitySelect;
    private static final int UPDATE_TODAY_WEATHER = 1;
    private static final String TAG = "MyApp";


    private TextView cityTv, timeTv, humidityTv,weekTv,pmDataTv, pmQualityTv, tempertureTv,
            climateTv, windTv,cur_temperature;
    private ImageView weatherImg, pmImg;

    // 定义主线程的 handler
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(android.os.Message msg) {
            switch(msg.what){
                case UPDATE_TODAY_WEATHER:
                    try {
                        updateTodayWeather((TodayWeather)msg.obj);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    break;
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.weather_info);

        Log.d(TAG, "MainActivity->OnCreate");

        mUpdateBtn = (ImageView)findViewById(R.id.title_update_btn);
        mUpdateBtn.setOnClickListener(this);

        mCitySelect = (ImageView)findViewById(R.id.title_city_manager);
        mCitySelect.setOnClickListener(this);

        // 调用initView初始化界面
        initView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {

        // 从 sharedpreferences 中读取 id ,拼接 url 地址
        if (view.getId() == R.id.title_update_btn) {
            SharedPreferences sharedPreferences = getSharedPreferences("config", MODE_PRIVATE);
            String cityCode = sharedPreferences.getString("main_city_code", "101010100");
            Log.d("myWeather", cityCode);

            // 在访问网络资源时，先检查一下网络状态是否可用
            if(NetUtil.getNetworkState(this) != NetUtil.NETWORN_NONE) {
                Log.d("myWeather", "网络 OK");
                queryWeatherCode(cityCode);
//                Toast.makeText(MainActivity.this, "网络好棒", Toast.LENGTH_LONG).show();
            }else {
                Log.d("myWeather", "网络挂了");
                Toast.makeText(MainActivity.this, "网络挂了", Toast.LENGTH_LONG).show();
            }
            queryWeatherCode(cityCode);
        }
        else if (view.getId() == R.id.title_city_manager) {
            Intent i = new Intent(this, SelectCity.class);
            startActivity(i);
        }
    }

    /**
     *  根据编号查询对应天气
     *  @param cityCode
     */
    private void queryWeatherCode(final String cityCode) {

//        String address = "http://wthrcdn.etouch.cn/WeatherApi?citykey=" + cityCode;
//        Log.d("myWeather", address);


        // 根据获取的地址，通过 httpclient 类(过时了，改用url)，得到相应的网络数据，网络数据 gzip 格式压缩，需解压
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection urlConnection = null;
                try {
//                    HttpClient httpClient = new DefaultHttpClient();
                    URL url = new URL("http://wthrcdn.etouch.cn/WeatherApi?citykey=" + cityCode);
                    Log.d("myWeather", url.toString());

                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.setConnectTimeout(8000);
                    urlConnection.setReadTimeout(8000);

                    InputStream in = urlConnection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));

                    StringBuilder response = new StringBuilder();
                    String str;

                    while((str=reader.readLine()) != null){
                        response.append(str);
                    }

                    String responseStr = response.toString();
                    Log.d("myWeather", responseStr);
                    TodayWeather todayWeather = parseXML(responseStr);
                    if (todayWeather != null) {
                        Log.d("myapp", todayWeather.toString());

                        // 发送消息，由主线程更新UI
                        Message msg =new Message();
                        msg.what = UPDATE_TODAY_WEATHER;
                        msg.obj = todayWeather;
                        mHandler.sendMessage(msg);
                    }

                }catch (Exception e) {
                    e.printStackTrace();
                }finally {
                    if (urlConnection != null){
                        urlConnection.disconnect();
                    }
                }
            }
        }).start();
    }

    // 解析xml
    private TodayWeather parseXML(String xmldata) {

        TodayWeather todayWeather = null;

        try{
            int fengxiangCount = 0;
            int fengliCount = 0;
            int dateCount = 0;
            int highCount = 0;
            int lowCount = 0;
            int typeCount = 0;

            XmlPullParserFactory fac = XmlPullParserFactory.newInstance();
            XmlPullParser xmlPullParser = fac.newPullParser();
            xmlPullParser.setInput(new StringReader(xmldata));

            int eventType =xmlPullParser.getEventType();
            Log.d("myapp", "parseXML");

            while(eventType != XmlPullParser.END_DOCUMENT){
                switch(eventType) {

                    // 判断当前事件是否为文档开始事件
                    case XmlPullParser.START_DOCUMENT:
                        break;

                    // 判断是否为标签元素开始事件
                    case XmlPullParser.START_TAG:
                        if(xmlPullParser.getName().equals("resp")){
                            todayWeather = new TodayWeather();
                        }
                        if (todayWeather != null){
                            if(xmlPullParser.getName().equals("city")){
                                eventType = xmlPullParser.next();
                                todayWeather.setCity(xmlPullParser.getText());
                                Log.d("myapp", "city" + xmlPullParser.getText());
                            }else if(xmlPullParser.getName().equals("updatetime")){
                                eventType = xmlPullParser.next();
                                todayWeather.setUpdatetime(xmlPullParser.getText());
                                Log.d("myapp", "updatetime" + xmlPullParser.getText());
                            }else if(xmlPullParser.getName().equals("shidu")){
                                eventType = xmlPullParser.next();
                                todayWeather.setShidu(xmlPullParser.getText());
                                Log.d("myapp", "shidu" + xmlPullParser.getText());
                            }else if(xmlPullParser.getName().equals("wendu")){
                                eventType = xmlPullParser.next();
                                todayWeather.setWendu(xmlPullParser.getText());
                                Log.d("myapp", "wendu" + xmlPullParser.getText());
                            }else if(xmlPullParser.getName().equals("pm25")){
                                eventType = xmlPullParser.next();
                                todayWeather.setPm25(xmlPullParser.getText());
                                Log.d("myapp", "pm2.5" + xmlPullParser.getText());
                            }else if(xmlPullParser.getName().equals("quality")){
                                eventType = xmlPullParser.next();
                                todayWeather.setQuality(xmlPullParser.getText());
                                Log.d("myapp", "quality" + xmlPullParser.getText());
                            }else if(xmlPullParser.getName().equals("fengxiang") && fengxiangCount == 0){
                                eventType = xmlPullParser.next();
                                todayWeather.setFengxiang(xmlPullParser.getText());
                                Log.d("myapp", "fengxiang" + xmlPullParser.getText());
                                fengxiangCount++;
                            }else if(xmlPullParser.getName().equals("fengli") && fengliCount == 0){
                                eventType = xmlPullParser.next();
                                todayWeather.setFengli(xmlPullParser.getText());
                                Log.d("myapp", "fengli" + xmlPullParser.getText());
                                fengliCount++;
                            }else if(xmlPullParser.getName().equals("date") && dateCount == 0){
                                eventType = xmlPullParser.next();
                                todayWeather.setDate(xmlPullParser.getText());
                                Log.d("myapp", "date" + xmlPullParser.getText());
                                dateCount++;
                            }else if(xmlPullParser.getName().equals("high") && highCount == 0){
                                eventType = xmlPullParser.next();
                                todayWeather.setHigh(xmlPullParser.getText().substring(2).trim());
                                Log.d("myapp", "high" + xmlPullParser.getText().substring(2).trim());
                                highCount++;
                            }else if(xmlPullParser.getName().equals("low") && lowCount == 0){
                                eventType = xmlPullParser.next();
                                todayWeather.setLow(xmlPullParser.getText().substring(2).trim());
                                Log.d("myapp", "low" + xmlPullParser.getText());
                                lowCount++;
                            }else if(xmlPullParser.getName().equals("type") && typeCount == 0){
                                eventType = xmlPullParser.next();
                                todayWeather.setType(xmlPullParser.getText());
                                Log.d("myapp", "type" + xmlPullParser.getText());
                                typeCount++;
                            }
                        }

                        break;

                    // 判断是否为标签元素结尾事件
                    case XmlPullParser.END_TAG:
                        break;
                }

                // 进入下一个元素并触发相应事件
                eventType = xmlPullParser.next();

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return todayWeather;
    }

    // 初始化控件
    void initView(){
        cityTv = (TextView) findViewById(R.id.city);
        timeTv = (TextView) findViewById(R.id.time);
        humidityTv = (TextView) findViewById(R.id.humidity);
        weekTv = (TextView) findViewById(R.id.week_today);
        pmDataTv = (TextView) findViewById(R.id.pm_data);
        pmQualityTv = (TextView) findViewById(R.id.pm2_5_quality);
        pmImg = (ImageView) findViewById(R.id.pm2_5_img);
        tempertureTv = (TextView) findViewById(R.id.temperature);
        climateTv = (TextView) findViewById(R.id.climate);
        windTv = (TextView) findViewById(R.id.wind);
        weatherImg = (ImageView) findViewById(R.id.weather_img);
        cur_temperature = (TextView) findViewById(R.id.cur_temperature);

        cityTv.setText("N/A");
        timeTv.setText("N/A");
        humidityTv.setText("N/A");
        weekTv.setText("N/A");
        pmDataTv.setText("N/A");
        pmQualityTv.setText("N/A");
        tempertureTv.setText("N/A");
        climateTv.setText("N/A");
        windTv.setText("N/A");
        cur_temperature.setText("N/A");
    }

    // 更新天气
    void updateTodayWeather (TodayWeather todayWeather) throws UnsupportedEncodingException {
        Log.d("myapp3", todayWeather.toString());
        Integer pm25 = Integer.valueOf(todayWeather.getPm25());

        cityTv.setText(todayWeather.getCity());
        timeTv.setText(todayWeather.getUpdatetime() + URLDecoder.decode("发布", "UTF-8"));
        humidityTv.setText("湿度:" + todayWeather.getShidu());
        weekTv.setText(todayWeather.getDate());
        pmDataTv.setText(todayWeather.getPm25());
        pmQualityTv.setText(todayWeather.getQuality());
        tempertureTv.setText(todayWeather.getLow() + "~" + todayWeather.getHigh());
        climateTv.setText(todayWeather.getType());
        windTv.setText("风力:" + todayWeather.getFengli());
        cur_temperature.setText("温度:" + todayWeather.getWendu() + "℃");
        switch(todayWeather.getType()){
            case "晴":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_qing);
                break;
            case "暴雪":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_baoxue);
                break;
            case "暴雨":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_baoyu);
                break;
            case "大暴雨":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_dabaoyu);
                break;
            case "大雪":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_daxue);
                break;
            case "大雨":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_dayu);
                break;
            case "多云":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_duoyun);
                break;
            case "雷阵雨":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_leizhenyu);
                break;
            case "雷阵雨冰雹":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_leizhenyubingbao);
                break;
            case "沙尘暴":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_shachenbao);
                break;
            case "雾":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_wu);
                break;
            case "小雪":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_xiaoxue);
                break;
            case "小雨":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_xiaoyu);
                break;
            case "阴":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_yin);
                break;
            case "雨夹雪":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_yujiaxue);
                break;
            case "特大暴雨":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_tedabaoyu);
                break;
            case "阵雪":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_zhenxue);
                break;
            case "阵雨":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_zhenyu);
                break;
            case "中雪":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_zhongxue);
                break;
            case "中雨":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_zhongyu);
                break;
        }

        if (pm25 >= 1 && pm25 <= 50) {
            pmImg.setImageResource(R.drawable.biz_plugin_weather_0_50);
        }
        else if (pm25 >= 51 && pm25 <=100) {
            pmImg.setImageResource(R.drawable.biz_plugin_weather_51_100);
        }
        else if (pm25 >= 101 && pm25 <=150) {
            pmImg.setImageResource(R.drawable.biz_plugin_weather_101_150);
        }
        else if (pm25 >= 151 && pm25 <= 200) {
            pmImg.setImageResource(R.drawable.biz_plugin_weather_151_200);
        }
        else if (pm25 >= 200 && pm25 <= 300) {
            pmImg.setImageResource(R.drawable.biz_plugin_weather_201_300);
        }
        else if (pm25 > 300) {
            pmImg.setImageResource(R.drawable.biz_plugin_weather_greater_300);
        }

        Toast.makeText(MainActivity.this, "更新成功！", Toast.LENGTH_LONG).show();

    }

}
