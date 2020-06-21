package com.example.aadproject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "JoggingApp.db";
    private static final String TABLE_NAME = "JoggingSessionTable";

    public DatabaseHelper(Context context){
        super(context, DATABASE_NAME, null, 1);
        SQLiteDatabase db = this.getReadableDatabase();
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    public ArrayList<HashMap<String, String>> getAllJoggingSessions(){
        SQLiteDatabase database = this.getWritableDatabase();
        ArrayList<HashMap<String,String>> sessionList = new ArrayList<>();
        Cursor cursor = database.rawQuery("select * from " + TABLE_NAME, null);

        ArrayList<String> sessionLists = new ArrayList<>();
        while(cursor.moveToNext()){
            HashMap<String,String> sessionInfo =  new HashMap<>();
            String date = cursor.getString(cursor.getColumnIndex("DATE"));
            date = dateToString(date);
            sessionInfo.put("date", date);

            sessionInfo.put("time", cursor.getString(cursor.getColumnIndex("TIME")));
            sessionInfo.put("duration", cursor.getString(cursor.getColumnIndex("DURATION")));

            Double distance =  cursor.getDouble(cursor.getColumnIndex("DISTANCE"));
            String distanceString = distanceToString(distance);
            sessionInfo.put("distance", distanceString);

            sessionList.add(sessionInfo);
        }
        Collections.reverse(sessionList);
        return sessionList;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private String dateToString(String dateString){
        Log.d("dateToString","Start" + dateString);
        String date = dateString.substring(8,10);
        String month = dateString.substring(5,7);
        String year = dateString.substring(0,4);
        String newDateString = date + "-" + month + "-" + year;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        LocalDate newDate= LocalDate.parse(newDateString, formatter);

        DateTimeFormatter dayDateFormatter = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy");
        String dayDateString = dayDateFormatter.format(newDate);

        return dayDateString;
    }

    private String distanceToString(Double distance){
        String newDistance = distance +" km";
        return newDistance;
    }
}
