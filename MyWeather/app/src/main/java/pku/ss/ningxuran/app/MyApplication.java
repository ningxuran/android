package pku.ss.ningxuran.app;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Environment;
import android.os.Parcelable;
import android.util.Log;
import android.widget.ListView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import pku.ss.ningxuran.bean.City;
import pku.ss.ningxuran.db.CityDB;
import pku.ss.ningxuran.myweather.R;
import pku.ss.ningxuran.myweather.SelectCity;

/**
 * Created by ningxuran on 15/10/17.
 */
public class MyApplication extends Application {
    private static final String TAG = "MyApp";
    private static Application mApplication;

    private List<City> mCityList;
    private CityDB mCityDB;

    @Override
    public void onCreate(){
        super.onCreate();

        Log.d(TAG, "MyApplication->OnCreate");
        mApplication = this;

        mCityDB = openCityDB();
        initCityList();
    }

    public static Application getInstance () {
        return mApplication;
    }

    // 创建初始化数据库
    private CityDB openCityDB () {
        String path = "/data"
                + Environment.getDataDirectory().getAbsolutePath()
                + File.separator + getPackageName()
                + File.separator + "databases"
                + File.separator
                + CityDB.CITY_DB_NAME;
        File db = new File(path);

        Log.d(TAG, path);

        if (!db.exists()) {
            Log.i("MyApp", "db is not exists");
            try{
                InputStream is = getAssets().open("city.db");
                FileOutputStream fos = new FileOutputStream(db);
                int len = -1;
                byte[] buffer = new byte[1024];
                while((len = is.read(buffer)) != -1){
                    fos.write(buffer, 0,len);
                    fos.flush();
                }
                fos.close();
                is.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(0);
            }
        }

        return new CityDB(this, path);
    }

    // 初始化城市信息列表
    private void initCityList() {
        mCityList = new ArrayList<City>();

        new Thread(new Runnable() {
            @Override
            public void run() {
                prepareCityList();
            }
        }).start();

    }

    private boolean prepareCityList (){
        mCityList = mCityDB.getAllCity();
        List<String> cityNameList = new ArrayList<>();
        for(City city: mCityList) {
            String cityName = city.getCity();
            cityNameList.add(cityName);
            Log.d(TAG, cityName);
        }

//        Intent intent = new Intent(this,SelectCity.class);
//        intent.putStringArrayListExtra("cityNameList", (ArrayList<String>) cityNameList);
//        startActivity(intent);

        return true;
    }
}
