package com.overhere.liz.overhere;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.overhere.liz.overhere.controller.AppController;
import com.overhere.liz.overhere.entities.AndroidUser;
import com.overhere.liz.overhere.entities.UserEvent;
import com.overhere.liz.overhere.http.HttpSender;
import com.overhere.liz.overhere.http.RetrofitInterface;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import retrofit.GsonConverterFactory;
import retrofit.Retrofit;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MainActivity";
    private AppController controller;
    private GoogleMap mGoogleMap;
    private Marker currLocationMarker;
    private ArrayList<LatLng> gpsPath = new ArrayList<>();
    private Handler handler = new Handler();
    private Runnable runnable = new Runnable(){

        @Override
        public void run(){

            if(controller.getCurrentBestLocation() != null){
                Location currentBestLocation = controller.getCurrentBestLocation();
                updateMap(currentBestLocation);
            }
            //Gets location every 5 seconds
            handler.postDelayed(runnable,5000);
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getApplicationContext();

        if(controller == null)
            controller = AppController.getInstance(this);

        if (Build.VERSION.SDK_INT == 23)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.INTERNET,
                            Manifest.permission.ACCESS_NETWORK_STATE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.SEND_SMS,
                            Manifest.permission.WAKE_LOCK,
                            Manifest.permission.VIBRATE,
                            Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.CALL_PHONE,
                            Manifest.permission.READ_EXTERNAL_STORAGE}, 1234);
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Wait 30 seconds before requesting location updates
        handler.postDelayed(runnable,30000);

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {

        //Add to shared Preferences for next login
        SharedPreferences pref = getApplicationContext().getSharedPreferences("PREFERENCES", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt("user_id",AndroidUser.getInstance().getUser_id());
        editor.putString("username", AndroidUser.getInstance().getUsername());
        editor.putString("name", AndroidUser.getInstance().getName());
        editor.putString("password", AndroidUser.getInstance().getPassword());
        editor.putString("carerNumber",AndroidUser.getInstance().getCarerNumber());
        if(AndroidUser.getInstance().getHomeAddress() != null)
        {
            editor.putFloat("latitude",AndroidUser.getInstance().getHomeAddress().getLatitude());
            editor.putFloat("longitude",AndroidUser.getInstance().getHomeAddress().getLongitude());
        }
        editor.apply();
        super.onStop();
    }

    @Override
    protected void onResume(){
       Log.d(TAG,"On Resume Called");
        super.onResume();

        //Has logged in before but has opened the app again so need to get the rest of the information
        SharedPreferences pref = getApplicationContext().getSharedPreferences("PREFERENCES", MODE_PRIVATE);
        Log.d(TAG,"User ID - " + String.valueOf(AndroidUser.getInstance().getUser_id()));
        if(AndroidUser.getInstance().getUser_id() == 0)
        {
            AndroidUser.getInstance().setPassword(pref.getString("password", null));
            AndroidUser.getInstance().setUsername(pref.getString("username", null));

            Retrofit retrofit = new Retrofit.Builder().baseUrl(getString(R.string.webUrl))
                    .addConverterFactory(GsonConverterFactory.create()).build();
            RetrofitInterface restInt = retrofit.create(RetrofitInterface.class);
            HttpSender httpSender = new HttpSender(restInt); //sends http requests to rest service

            httpSender.getUser(AndroidUser.getInstance());
            Log.d(TAG,"User ID - " + AndroidUser.getInstance());

        }

        int response = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if(response != ConnectionResult.SUCCESS){
            Log.d(TAG,"Google play services not available - user must download it");
            GoogleApiAvailability.getInstance().getErrorDialog(this,response,1).show();
        } else {
            Log.d(TAG,"Google Play Services is available - no action is required");
        }


        if(controller == null)
            controller = AppController.getInstance(this);

        //if(!controller.getApiClient().isConnected())
            controller.getApiClient().disconnect();
            controller.getApiClient().connect();

        //Wait 10 seconds before requesting location updates
        handler.postDelayed(runnable,5000);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mGoogleMap.setBuildingsEnabled(true);
        mGoogleMap.setIndoorEnabled(true);

        FloatingActionButton fab3 = (FloatingActionButton) findViewById(R.id.fabHeartAlert);
        fab3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Snackbar.make(view, "Sending Heart Message! Hold in there!", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                sendAlertText();
            }});
    }

    public void updateMap(Location currentBestLocation){

        if(currentBestLocation != null){
            if(gpsPath.isEmpty()){
                CircleOptions circleOptions = new CircleOptions()
                        .strokeColor(Color.RED)
                        .fillColor(Color.TRANSPARENT)
                        .center(new LatLng(currentBestLocation.getLatitude(),currentBestLocation.getLongitude()))
                        .radius(50);
                mGoogleMap.addCircle(circleOptions);
            }

            ((TextView) findViewById(R.id.lastUpdatedTimeValue)).setText(String.valueOf(DateFormat.getTimeInstance().format(new Date())));
            LatLng oldPosition;
            if (currLocationMarker != null) {
                oldPosition = currLocationMarker.getPosition();
                currLocationMarker.remove();
                gpsPath.add(oldPosition);
            }

            LatLng latLng = new LatLng(currentBestLocation.getLatitude(), currentBestLocation.getLongitude());
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title("Current Position");
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            updateMapPath(gpsPath);
            currLocationMarker = mGoogleMap.addMarker(markerOptions);

            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,18));
        }

    }

    //Draw the path of the user
    public void updateMapPath(ArrayList<LatLng> gpsPath){

        if (mGoogleMap == null)
            return;

        if (gpsPath.size() < 2)
            return;

        PolylineOptions options = new PolylineOptions();

        options.color(Color.parseColor("#CC0000FF"));
        options.width(5);
        options.visible(true );

        for (int i = 0; i < gpsPath.size();i++)
        {
            LatLng latlngRecorded = gpsPath.get(i);
            if(i >0){
                if(latlngRecorded != gpsPath.get(i-1)){
                    options.add(latlngRecorded);
                }
            }

        }

        mGoogleMap.addPolyline(options);

    }

    public void sendAlertText() {

        Log.d(TAG, "Sending heart Alert");
        SmsManager smsManager = SmsManager.getDefault();
        String[] numbers = {};
        String message = AndroidUser.getInstance().getName() + " is having some heart pain and may need help!";
        message += ". Their last known location is: \n";
        message += "https://maps.google.com/?q=" + AndroidUser.getInstance().getLastKnownLocation().getLatitude()
                + "," + AndroidUser.getInstance().getLastKnownLocation().getLongitude();
        message += ",17z . Please pass these coordinates to emergency services if needed."; // zoom level
        String number = AndroidUser.getInstance().getCarerNumber();
        if (number.contains(",")) {
            numbers = number.split(",");
        } else {
            numbers[0] = number;
        }
        if (numbers != null) {
            for (String no : numbers) {
                smsManager.sendTextMessage(no, null, message, null, null);
            }
            Log.d(TAG, "Texts sent!");
        }


        Retrofit retrofit = new Retrofit.Builder().baseUrl(getString(R.string.webUrl)) //url for web service
                .addConverterFactory(GsonConverterFactory.create()).build();
        RetrofitInterface restInt = retrofit.create(RetrofitInterface.class);
        HttpSender httpSender = new HttpSender(restInt);

        UserEvent ue = new UserEvent();
        ue.setUser_id(AndroidUser.getInstance().getUser_id());
        ue.setEvent_type("Heart");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ENGLISH);
        String strDate = df.format(new Date());
        ue.setTimestamp(strDate);
        ue.setLastLocation(AndroidUser.getInstance().getLastKnownLocation());
        Log.d(TAG, "Sending event information");
        httpSender.httpSendUserEventInformation(ue);

        Log.d(TAG, "Prompt to be displayed");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Would like to ring emergency services?")
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        Intent callIntent = new Intent(Intent.ACTION_CALL);
                        callIntent.setData(Uri.parse("tel:999"));
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED &&
                                ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        startActivity(callIntent);
                    }
                })
                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Don't ring emergency services
                    }
                })
                .setIcon(R.drawable.heart_beat)
                .show();
    }
}