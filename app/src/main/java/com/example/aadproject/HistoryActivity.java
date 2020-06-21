package com.example.aadproject;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;

public class HistoryActivity extends AppCompatActivity {

    ImageButton homeButton;
    DatabaseHelper databaseHelper;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //make activity fullscreen
        setContentView(R.layout.activity_history);
        getSupportActionBar().hide();
        Window w = getWindow();
        w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        databaseHelper = new DatabaseHelper(this);
        setListView();

        homeButton = findViewById(R.id.homeImageButton);
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HistoryActivity.this, MapsActivity.class));
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onResume() {
        Log.d("onResume","onResume Called");
        super.onResume();
        setListView();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void setListView(){
        ArrayList<HashMap<String,String>> sessionList = databaseHelper.getAllJoggingSessions();
        ListView listView = findViewById(R.id.joggingSessionsList);
        View emptyView = findViewById(R.id.empty);

        if (sessionList.size() > 0 ){
            emptyView.setVisibility(View.GONE);
            ListAdapter adapter = new SimpleAdapter(HistoryActivity.this, sessionList,
                    R.layout.list_history, new String[]{"date","time", "duration", "distance"},
                    new int[]{R.id.dateTextView, R.id.timeTextView, R.id.durationTextView, R.id.totalDistanceTextView});
            listView.setAdapter(adapter);
        }
        else{
            listView.setEmptyView(emptyView);
        }
    }
}
