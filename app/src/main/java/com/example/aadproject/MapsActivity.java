package com.example.aadproject;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.ButtCap;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.Arrays;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;

public class MapsActivity extends AppCompatActivity implements LocationListener, OnMapReadyCallback {

    private GoogleMap mMap;
    private DatabaseReference databaseReferences;
    private LocationListener locationListener;
    private LocationManager locationManager;
    private static final int LOCATION_PERMISSION_REQUEST = 1;
    private static final int UPDATE_INTERVAL = 10000; //10 seconds
    private static final int FASTEST_INTERVAL = 3000; //5 seconds
    private LatLng lastLocation;
    private LatLng newLocation;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private boolean isTracking = false;
    Button startButton;
    Button pauseButton;
    Button stopButton;
    TextView distanceTextView;
    TextView timerTextView;
    private LocationCallback locationCallback;
    private int totalDistance = 0; //in meters
    private static DecimalFormat decimalFormat = new DecimalFormat("0.00");
    long startTimeMillis = 0;
    Handler timerHandler;
    Runnable timerRunnable;
    Timer timer;
    DatabaseHelper databaseHelper;
    String startTime;
    String startDate;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//will hide the title
        getSupportActionBar().hide();
        Window w = getWindow();
        w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        databaseHelper = new DatabaseHelper(this);

//        mMap.setMinZoomPreference(30);
        setContentView(R.layout.activity_maps);
        startButton = findViewById(R.id.buttonStart);
        pauseButton = findViewById(R.id.buttonPause);
        stopButton = findViewById(R.id.buttonStop);
        distanceTextView = findViewById(R.id.distanceTextView);
        timerTextView = findViewById(R.id.timerTextView);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {


                if (locationResult == null) {
                    return;
                } else {
                    Log.d("location callback", "onlocationresult");
                    lastLocation = newLocation;
                    newLocation = setLocationLatLng(locationResult.getLastLocation());
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


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);


        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int result = apiAvailability.isGooglePlayServicesAvailable(this);

        if (result != ConnectionResult.SUCCESS) {
            if (!apiAvailability.isUserResolvableError(result)) {
                Toast.makeText(this, "Google Play Services not Available", Toast.LENGTH_LONG).show();
            }
        }
        startButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {

                mMap.clear();
                startTime = getCurrentTime();
                startDate = getCurrentDate();

                startTimeMillis = System.currentTimeMillis();
                timer = new Timer(startTimeMillis, timerTextView);
                timer.startTimer();

                trackLocation();
                drawStartingPoint(newLocation);
                pauseButton.setText("Pause");
                pauseButton.setVisibility(View.VISIBLE);
                stopButton.setVisibility(View.VISIBLE);
                startButton.setVisibility(View.GONE);
                isTracking = true;

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
                pauseButton.setVisibility(View.GONE);
                stopButton.setVisibility(View.GONE);
                startButton.setVisibility(View.VISIBLE);
                pauseButton.setText("Continue");

                saveToDatabase();

                isTracking = false;
            }
        });
        pauseButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //Pause
                if (isTracking) {
                    timer.pauseTimer();
                    stopLocationTracking();

                    pauseButton.setText("Continue");
                    isTracking = false;

                    //Continue
                } else {
                    timer.resumeTimer();
                    retrieveLastLocation();
                    trackLocation();
                    pauseButton.setText("Pause");
                    isTracking = true;

                }

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);

        if (permission == PERMISSION_GRANTED) {
            Log.d("onStart", "Permission Granted");
            retrieveLastLocation();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, LOCATION_PERMISSION_REQUEST);
        }

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
            Log.d("setLocationLatLng", "location not null");
            LatLng latLngLocation = new LatLng(location.getLatitude(), location.getLongitude());
            Log.d("setLocationLatLng", "finish" + latLngLocation.latitude + latLngLocation.longitude);
            return latLngLocation;
        } else {
            Log.d("setLocationLatLng", "Location null");
            return new LatLng(0, 0);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setScrollGesturesEnabled(false);
        mMap.getUiSettings().setScrollGesturesEnabledDuringRotateOrZoom(false);


    }

    private void moveCamera(LatLng latLng) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
    }

    public void trackLocation() {
        Log.d("trackLocation()", "startTracking");
        //change comments
        // Create the location request to start receiving updates
        final LocationRequest locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);

        // Create LocationSettingsRequest object using location request
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        // Check whether location settings are satisfied
        // https://developers.google.com/android/reference/com/google/android/gms/location/SettingsClient
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);

        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());


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


    public void startButtonOnClick(View view) {
//        final Button testButton = findViewById(R.id.button);
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
    public void onLocationChanged(Location location) {

        Log.d("onLocationChanged", "Location changed!");
        lastLocation = setLocationLatLng(location);
        moveCamera(lastLocation);


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

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void saveToDatabase(){
            String duration = timerTextView.getText().toString();
            double distance = getDistance();

            databaseHelper.addJoggingSession(new JoggingSession(getCurrentDate(),getCurrentTime(),duration,distance));

    }
}