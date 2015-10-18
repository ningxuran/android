package pku.ss.ningxuran.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import pku.ss.ningxuran.bean.City;

/**
 * Created by ningxuran on 15/10/17.
 */
public class CityDB {
    public static final String CITY_DB_NAME = "city2.db";
    public static final String CITY_TABLE_NAME = "city";
    private SQLiteDatabase db;

    public CityDB (Context context, String path) {
        db = context.openOrCreateDatabase(CITY_DB_NAME, Context.MODE_PRIVATE, null);
    }

    public List<City> getAllCity () {
        List<City> list = new ArrayList<>();

        Cursor c = db.rawQuery("Select * from " + CITY_TABLE_NAME, null);

        while (c.moveToNext()) {
            String province = c.getString(c.getColumnIndex("province"));
            String city = c.getString(c.getColumnIndex("city"));
            String number = c.getString(c.getColumnIndex("number"));
            String allPY = c.getString(c.getColumnIndex("allpy"));
            String allFirstPY = c.getString(c.getColumnIndex("allfirstpy"));
            String firstPY = c.getString(c.getColumnIndex("firstpy"));

            Log.d("test", city);
            City item = new City(province,city,number,firstPY,allPY,allFirstPY);
            list.add(item);

        }
        return list;

    }
}
