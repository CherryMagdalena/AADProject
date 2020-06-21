package com.example.aadproject;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;


public class MapsActivity extends AppCompatActivity implements LocationListener, OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST = 1;
    private static final int UPDATE_INTERVAL = 10000; //10 seconds
    private static final int FASTEST_INTERVAL = 3000; //5 seconds
    private static DecimalFormat decimalFormat = new DecimalFormat("0.00");
    boolean isTracking = false;
    boolean isOnPause = false;
    int screenWidth;
    private GoogleMap mMap;
    private LatLng lastLocation;
    private LatLng newLocation;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private FloatingActionButton startButton;
    private ImageButton stopButton;
    private TextView distanceTextView;
    private TextView timerTextView;
    private LocationCallback locationCallback;
    private int totalDistance = 0; //in meters
    private long startTimeMillis = 0;
    private Timer timer;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //request permission to access location
        requestPermission();
        //make the activity fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        Window w = getWindow();
        w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        databaseHelper = new DatabaseHelper(this);

        setContentView(R.layout.activity_maps);
        startButton = findViewById(R.id.floatingActionButton);
        stopButton = findViewById(R.id.buttonStop);
        distanceTextView = findViewById(R.id.distanceTextView);
        timerTextView = findViewById(R.id.timerTextView);
        ImageButton historyButton = findViewById(R.id.historyImageButton);
        screenWidth = getResources().getConfiguration().screenWidthDp;

        //Set button image depending on the screen size
        if(screenWidth >= 600){
            startButton.setImageResource(R.drawable.run_icon_big);
            setBigIcon(startButton);
            stopButton.setImageResource(R.drawable.stop_icon);
            setBigIcon(stopButton);
        }
        else{
            startButton.setImageResource(R.drawable.run_icon);
            stopButton.setImageResource(R.drawable.stop_icon);
        }

        setLocationCallback();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        startButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                //pause
                if (isTracking){
                    timer.pauseTimer();
                    stopLocationTracking();
                    if(screenWidth >= 600){
                        startButton.setImageResource(R.drawable.play_icon_big);
                    } else{
                        startButton.setImageResource(R.drawable.play_icon);
                    }
                    isTracking = false;
                    isOnPause = true;

                } else{ //resume
                    if (isOnPause){
                        timer.resumeTimer();
                        retrieveLastLocation();
                        trackLocation();
                        if (screenWidth >= 600){
                            startButton.setImageResource(R.drawable.pause_icon_big);
                        }else{
                            startButton.setImageResource(R.drawable.pause_icon);
                        }
                        isTracking = true;
                        isOnPause = false;

                    } else{ //start
                        //reset
                        mMap.clear();
                        startTimeMillis = System.currentTimeMillis();
                        timer = new Timer(startTimeMillis, timerTextView);
                        timer.startTimer();
                        //start tracking
                        retrieveLastLocation();
                        trackLocation();
                        drawStartingPoint(newLocation);

                        stopButton.setVisibility(View.VISIBLE);
                        startButton.setImageResource(R.drawable.pause_icon);
                        isTracking = true;
                    }
                }
            }

        });
        stopButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                totalDistance = 0;
                timer.stopTimer();
                retrieveLastLocation();
                drawEndPoint(newLocation);
                stopLocationTracking();
                stopButton.setVisibility(View.GONE);
                saveToDatabase();
                isTracking = false;
                isOnPause = false;
                startButton.setImageResource(R.drawable.run_icon);
            }
        });
        //history button on the bottom AppBar
        historyButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MapsActivity.this,HistoryActivity.class ));
            }
        });
    }

    public void setLocationCallback(){
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {

                if (locationResult == null) {
                    Log.d("locationCallback", "locationResult null");
                    return;

                } else {
                    Log.d("location callback", "onlocationresult");
                    lastLocation = newLocation;
                    newLocation = setLocationLatLng(locationResult.getLastLocation());
                    moveCamera(newLocation);

                    if (isTracking){
                        if (lastLocation != null){
                            int distance = calculateDistanceBetween(lastLocation, newLocation);
                            totalDistance += distance;
                            Log.d("locationCallback", "total Distance = " + totalDistance);
                            distanceTextView.setText(decimalFormat.format(totalDistance / 1000.0) + " km");
                            drawPolyline(lastLocation, newLocation);
                        }
                    }
                }
            }
        };
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults[0] != PERMISSION_GRANTED) {
                Toast.makeText(this, "Location Permission Denied!", Toast.LENGTH_LONG).show();
            } else {
                retrieveLastLocation();
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(false);
        retrieveLastLocation();
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("onLocationChanged", "Location changed!");
        lastLocation = setLocationLatLng(location);
        moveCamera(lastLocation);
    }

    private void retrieveLastLocation() {
        Log.d("retrieveLastLocation", "start");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED) {
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(
                    new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            Log.d("onSuccess", "retrieveLocation");
                            newLocation = setLocationLatLng(location);
                            moveCamera(newLocation);
                        }
                    }
            );
        }
    }

    private LatLng setLocationLatLng(Location location) {
        if (location != null) {
            LatLng latLngLocation = new LatLng(location.getLatitude(), location.getLongitude());
            return latLngLocation;
        } else {
            return new LatLng(0, 0);
        }
    }

    private void moveCamera(LatLng latLng) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
    }

    //from https://guides.codepath.com/android/Retrieving-Location-with-LocationServices-API
    public void trackLocation() {
        final LocationRequest locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback,
                Looper.myLooper());
    }

    public void stopLocationTracking() {
        final Task<Void> voidTask = fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        voidTask.addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Log.d("stopLocationTracking()", "addOnCompleteListener: " + task.isComplete());
            }
        });

        voidTask.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d("stopLocationTracking()", "addOnSuccessListener: success");
                lastLocation= null;
            }
        });

        voidTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("stopLocationTracking", "addOnFailureListener: failed");
            }
        });
    }

    public void drawPolyline(LatLng latLng1, LatLng latLng2) {
        Polyline polyline = mMap.addPolyline(new PolylineOptions()
                .add(latLng1, latLng2)
        );
        polyline.setEndCap(new RoundCap());
        polyline.setWidth(15);
        polyline.setColor(0xff0000ff);
        polyline.setJointType(JointType.DEFAULT);
    }

    public void drawStartingPoint(LatLng startingPoint) {
        MarkerOptions options = new MarkerOptions()
                .position(startingPoint)
                .title("Starting point").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
        mMap.addMarker(options);
        moveCamera(newLocation);
    }

    public void drawEndPoint(LatLng endPoint) {
        MarkerOptions options = new MarkerOptions()
                .position(endPoint)
                .title("End point").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        mMap.addMarker(options);
        moveCamera(newLocation);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }
    @Override
    public void onProviderEnabled(String provider) {
    }
    @Override
    public void onProviderDisabled(String provider) {
    }

    public int calculateDistanceBetween(LatLng lastLocation, LatLng newLocation){
        float[] results = new float[1];
        Location.distanceBetween(lastLocation.latitude, lastLocation.longitude,
                newLocation.latitude, newLocation.longitude,
                results);
        int distance =(int) results[0];
        return distance;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public String getCurrentDate(){
        String currentDate = java.time.LocalDate.now().toString();
        return currentDate;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public String getCurrentTime(){
        String currentTime = java.time.LocalTime.now().toString();
        currentTime = currentTime.substring(0,5);
        return currentTime;
    }

    public double getDistance(){
        String distanceString =distanceTextView.getText().toString();
        distanceString = distanceString.substring(0, distanceString.length()-2);
        double distance = Double.parseDouble(distanceString);
        BigDecimal bigDecimal = new BigDecimal(distance).setScale(2, RoundingMode.HALF_UP);
        distance = bigDecimal.doubleValue();
        return distance;
    }

    public String durationToString(String duration){
        Log.d("durationToString","Start");
        String minutes = duration.substring(0,1);
        String second = duration.substring(2,4);
        String newDurationString;
        Log.d("durationToString",minutes);
        Log.d("durationToString",second);
        int minutesInInt = Integer.parseInt(minutes);
        if (minutesInInt > 60){
            String hour = String.valueOf(minutesInInt/60);
            minutes = String.valueOf(minutesInInt%60);
            newDurationString = hour + " hours " + minutes + " minutes " + second + " seconds";
            Log.d("durationToString", newDurationString);
            return newDurationString;
        }
        else{
            newDurationString = minutes + " minutes " + second + " seconds";
            Log.d("durationToString", newDurationString);
            return newDurationString;
        }
    }

    public void requestPermission(){
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permission == PERMISSION_GRANTED) {
            Log.d("requestPermission", "Permission Granted");
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, LOCATION_PERMISSION_REQUEST);
        }
    }

    public void setBigIcon(ImageButton imageButton){
        imageButton.setScaleType(ImageView.ScaleType.CENTER);
        imageButton.setAdjustViewBounds(true);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void saveToDatabase(){
        String duration = timerTextView.getText().toString();
        duration = durationToString(duration);
        double distance = getDistance();
        databaseHelper.addJoggingSession(new JoggingSession(getCurrentDate(),getCurrentTime(),duration,distance));
    }
}