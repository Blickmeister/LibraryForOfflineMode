package cz.fim.uhk.thesis.libraryforofflinemode.helper;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.jetbrains.annotations.Nullable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import cz.fim.uhk.thesis.libraryforofflinemode.model.User;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "user_management.db";
    private static final String TABLE_NAME = "user";

    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, 1);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + TABLE_NAME + "(user_id TEXT PRIMARY KEY, " +
                "user_latitude REAL," +
                "user_longitude REAL," +
                "user_is_online INTEGER," +
                "user_actual_state TEXT," +
                "user_future_state TEXT," +
                "user_first_conn_to_server TEXT," +
                "user_last_conn_to_server TEXT," +
                "user_sensor_information_temperature REAL," +
                "user_sensor_information_pressure REAL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS user");
        onCreate(db);
    }

    public boolean dropTable() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS user");
        return true;
    }

    public boolean insertData(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("user_id", user.getSsid());
        contentValues.put("user_latitude", user.getLatitude());
        contentValues.put("user_longitude", user.getLongitude());
        // int isOnlineForDb = usCon.isOnline() ? 1 : 0;
        contentValues.put("user_is_online", user.isOnline());
        contentValues.put("user_actual_state", user.getActualState());
        contentValues.put("user_future_state", user.getFutureState());
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        contentValues.put("user_first_conn_to_server", format.format(user.getFirstConnectionToServer()));
        contentValues.put("user_last_conn_to_server", format.format(user.getLastConnectionToServer()));
        contentValues.put("user_sensor_information_temperature", user.getTemperature());
        contentValues.put("user_sensor_information_pressure", user.getPressure());
        long result = db.insert(TABLE_NAME,null ,contentValues);
        return result != -1;
    }

    public boolean updateUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("user_id", user.getSsid());
        contentValues.put("user_latitude", user.getLatitude());
        contentValues.put("user_longitude", user.getLongitude());
        // int isOnlineForDb = usCon.isOnline() ? 1 : 0;
        contentValues.put("user_is_online", user.isOnline());
        contentValues.put("user_actual_state", user.getActualState());
        contentValues.put("user_future_state", user.getFutureState());
        contentValues.put("user_first_conn_to_server", user.getFirstConnectionToServer().toString());
        contentValues.put("user_last_conn_to_server", user.getLastConnectionToServer().toString());
        contentValues.put("user_sensor_information_temperature", user.getTemperature());
        contentValues.put("user_sensor_information_pressure", user.getPressure());
        db.update(TABLE_NAME, contentValues, "user_id = ?", new String[]{String.valueOf(user.getSsid())});
        return true;
    }

    public Cursor getAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.rawQuery("select * from " + TABLE_NAME,null);
    }

    public Cursor getUserById(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("select * from " + TABLE_NAME + " where user_id = '"+ id + "'",
                null);
        res.moveToFirst(); // user_id je unikátní (vždy jen jeden výsledek)
        return res;
    }

}
