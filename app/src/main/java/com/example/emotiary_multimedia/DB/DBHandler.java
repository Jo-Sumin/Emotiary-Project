package com.example.emotiary_multimedia.DB;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class DBHandler {
    private final String TAG = "DBHandler";
    SQLiteOpenHelper Helper = null;
    SQLiteDatabase DB = null;


    public DBHandler(Context context, String name) {
        Helper = new DBHelper(context, name, null, 1);
    }

    public static DBHandler open(Context context, String name) {
        return new DBHandler(context, name);
    }

    //select * from mytable
    public Cursor selectALL()
    {
        DB = Helper.getReadableDatabase();
        Cursor c = DB.query("mytable", null, null, null, null, null, null);
        return c;
    }

    public Cursor select(String date)
    {
        DB = Helper.getReadableDatabase();
        Cursor c = DB.rawQuery("select * from mytable where date =?",new String[]{date});
        return c;
    }

    public void insert(String date, String memo, String emotion) {

        Log.d(TAG, "insert");

        DB = Helper.getWritableDatabase();

        ContentValues value = new ContentValues();
        value.put("date", date);
        value.put("memo", memo);
        value.put("emotion", emotion);

        DB.insert("mytable", null, value);

    }

    public void update(String date, String memo, String emotion){
        Log.d(TAG, "update");

        DB = Helper.getWritableDatabase();

        ContentValues value = new ContentValues();
        value.put("memo",memo);
        value.put("emotion",emotion);
        DB.update("mytable",value,"date=?",new String[]{date});
    }

    public void delete(String date)
    {
        Log.d(TAG, "delete");
        DB = Helper.getWritableDatabase();
        DB.delete("mytable", "date=?", new String[]{date});
    }

    public void close() {
        Helper.close();
    }


}
