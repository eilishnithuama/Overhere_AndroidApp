package com.overhere.liz.overhere.controller;

import java.util.List;
import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.overhere.liz.overhere.entities.AndroidUser;
import com.overhere.liz.overhere.service.ActivityRecognitionService;
import com.overhere.liz.overhere.service.GeofenceService;
import com.overhere.liz.overhere.service.LocationService;

public class AppController implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener
{

    private static final String TAG = AppController.class.getSimpleName();
    private Context context;
    private static final String GEOFENCEID = "GeoFenceID";
    private GoogleApiClient mGoogleApiClient;
    private PendingIntent GeoFencePendingIntent;
    private GeofencingRequest geofenceRequest;

    private static AppController instance = null;

    private Handler handler = new Handler();

    private Runnable runnable = new Runnable() {
        public void run() {
            if(AndroidUser.getInstance().getLastKnownLocation() != null){
                Log.d(TAG,"Starting Geofence from runnable");
                startGeoMonitoring();
            }
            else{
                handler.postDelayed(this, 5000);
            }
        }
    };

    public static AppController getInstance(Context context){

        if(instance == null) {
            instance = new AppController(context.getApplicationContext());
        }
        return instance;
    }

    private AppController(Context context){
        Log.d(TAG,"Controller Started");
        this.context = context;
        buildApiClient();

    }

    public void setContext(Context context){
        this.context = context.getApplicationContext();
    }

    public Location getCurrentBestLocation(){
        Location loc = new Location("dummyLocation");
        loc.setLongitude(AndroidUser.getInstance().getLastKnownLocation().getLongitude());
        loc.setLatitude(AndroidUser.getInstance().getLastKnownLocation().getLatitude());
        return loc;
    }

    private void buildApiClient(){
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(ActivityRecognition.API)
                    .addApi(LocationServices.API).build();
        }
    }

    public GoogleApiClient getApiClient(){
        return mGoogleApiClient;
    }

    private void startGeoMonitoring() {

        Log.d(TAG,"startGeomonitoring");
        if(AndroidUser.getInstance().getHomeAddress() == null){
            AndroidUser.getInstance().setHomeAddress(AndroidUser.getInstance().getLastKnownLocation());
        }

        Geofence geofence = new Geofence.Builder()
                .setRequestId(GEOFENCEID)
                .setCircularRegion(AndroidUser.getInstance().getHomeAddress().getLatitude(),AndroidUser.getInstance().getHomeAddress().getLongitude(),100)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setNotificationResponsiveness(1000)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
        if(geofenceRequest == null){
            geofenceRequest = new GeofencingRequest.Builder()
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                    .addGeofence(geofence).build();
        }

        Intent intent = new Intent(context, GeofenceService.class);
        GeoFencePendingIntent = PendingIntent.getService(context,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);

        if(!mGoogleApiClient.isConnected()){
            Log.d(TAG,"Not connected to google api client");
            mGoogleApiClient.connect();
            Toast.makeText(context,"Google API Client not connected",Toast.LENGTH_SHORT).show();
        }
        else {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                return;
            }

            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,geofenceRequest,GeoFencePendingIntent)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(@NonNull Status status) {
                            if(status.isSuccess()){
                                Log.d(TAG,"Successfully added Geofence");
                                Toast.makeText(context,"Geofence started",Toast.LENGTH_SHORT).show();
                            }
                            else {
                                Log.d(TAG,"Failed to add Geofence : " + status.getStatus());
                            }
                        }
                    });
            Toast.makeText(context,"Geofence started",Toast.LENGTH_SHORT).show();
        }
    }

    public void deregisterGeofence(){
        LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient,GeoFencePendingIntent);
    }

    public void reRegisterGeofence(){
        if(geofenceRequest != null) {
            List geofences = geofenceRequest.getGeofences();
            //Check if the geofence has already been added
            Log.d(TAG,"Reregistering geofence");
            if (geofences.isEmpty()) {
                Location homeAddress = new Location("Home");
                homeAddress.setLongitude(AndroidUser.getInstance().getHomeAddress().getLongitude());
                homeAddress.setLatitude(AndroidUser.getInstance().getHomeAddress().getLatitude());
                float distance = getCurrentBestLocation().distanceTo(homeAddress);
                //if we are within 20 meters of home start geomonitoring again
                if (distance < 20) {
                    Log.d(TAG,"Restarting geofence monitoring");
                    startGeoMonitoring();
                }
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG,"onConnected: Google API Client Connected");

        //Start Location monitoring
        Intent locIntent = new Intent(context,LocationService.class);
        context.startService(locIntent);

        //Start Activity Monitoring
        //Intent intent = new Intent(context, ActivityRecognitionService.class);
        //PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT );
        //ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient, 3000, pendingIntent);

        //Start Geofence
        handler.postDelayed(runnable, 5000);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    public boolean isInternetAvailable() {
        if (context != null) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            return connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnectedOrConnecting();
        } else {
            return false;
        }
    }
}
