package com.example.aadproject;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "JoggingApp.db";
    public static final String TABLE_NAME = "UserJoggingDatabase";

    public DatabaseHelper(Context context){
        super(context, DATABASE_NAME, null, 1);
//        SQLiteDatabase db = this.getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (ID INTEGER PRIMARY KEY, DATE TEXT, TIME TEXT, DURATION TEXT, DISTANCE FLOAT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void addJoggingSession(JoggingSession joggingSession){
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put("DATE", joggingSession.getDate());
        contentValues.put("TIME", joggingSession.getTime());
        contentValues.put("DURATION", joggingSession.getDuration());
        contentValues.put("DISTANCE", joggingSession.getDistance());

        long result = sqLiteDatabase.insert(TABLE_NAME,null, contentValues);

        if (result > 0){
            Log.d("databaseHelper", "inserted successfully");
        } else{
            Log.d("databaseHelper", "failed to insert");
        }
        sqLiteDatabase.close();

    }
}
