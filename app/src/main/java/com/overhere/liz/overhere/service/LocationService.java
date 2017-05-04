package com.overhere.liz.overhere.service;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.overhere.liz.overhere.R;
import com.overhere.liz.overhere.controller.AppController;
import com.overhere.liz.overhere.entities.AndroidUser;
import com.overhere.liz.overhere.entities.GPSLocation;
import com.overhere.liz.overhere.http.HttpSender;
import com.overhere.liz.overhere.http.RetrofitInterface;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit.GsonConverterFactory;
import retrofit.Retrofit;

public class LocationService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    public static final String TAG = LocationService.class.getSimpleName();
    private static final int TWO_MINUTES = 1000 * 60 * 2;
    private Location currentBestLocation;
    private GoogleApiClient mGoogleApiClient;
    private HttpSender httpSender;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Retrofit retrofit = new Retrofit.Builder().baseUrl(getString(R.string.webUrl)) //url for web service
                .addConverterFactory(GsonConverterFactory.create()).build();
        RetrofitInterface restInt = retrofit.create(RetrofitInterface.class);
        httpSender = new HttpSender(restInt);
        connectToGoogleApiClient();

        return START_STICKY;
    }

    public void connectToGoogleApiClient(){
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addApi(ActivityRecognition.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            if (!mGoogleApiClient.isConnected() || !mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
        } else {
            Log.e(TAG, "Unable to connect to Google Play Services.");
        }
    }

    private void locationUpdated(Location location)
    {
        //Toast.makeText(getApplicationContext(),"Location Changed",Toast.LENGTH_SHORT).show();
        GPSLocation gps = new GPSLocation();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ENGLISH);
        String strDate = df.format(new Date());
        if (isBetterLocation(location, currentBestLocation)) {

            gps.setLongitude((float) location.getLongitude());
            gps.setLatitude((float) location.getLatitude());
            gps.setTimestamp(strDate);
            currentBestLocation = location;
        } else {
            gps.setLongitude((float) currentBestLocation.getLongitude());
            gps.setLatitude((float) currentBestLocation.getLatitude());
            gps.setTimestamp(strDate);
        }

        gps.setUser_id(AndroidUser.getInstance().getUser_id());
        AndroidUser.getInstance().setLastKnownLocation(gps);
        if(AndroidUser.getInstance().getUser_id() != 0){
            httpSender.httpSendGPSInformation(gps);
        }
    }

    private boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if(isLocationEnabled(getApplicationContext())){
            currentBestLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            GPSLocation gps = new GPSLocation();
            gps.setLatitude((float)currentBestLocation.getLatitude());
            gps.setLongitude((float)currentBestLocation.getLongitude());
            AndroidUser.getInstance().setLastKnownLocation(gps);
            if (currentBestLocation != null) {
                Log.d(TAG,"onConnected: Current best location set");
                startLocMonitoring();
            }
        }
        else{
            // notify the user that location needs to be turned on
            AlertDialog.Builder dialog = new AlertDialog.Builder(getApplicationContext());
            dialog.setMessage(getApplicationContext().getResources().getString(R.string.location_not_enabled));
            dialog.setPositiveButton(getApplicationContext().getResources().getString(R.string.open_location_settings), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {

                    Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    getApplicationContext().startActivity(myIntent);
                }
            });
            dialog.setNegativeButton(getApplicationContext().getString(R.string.prompt_cancel), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                }
            });
            dialog.show();
        }
        //Start Activity Monitoring once location has been recieved
        Intent intent = new Intent(getApplicationContext(), ActivityRecognitionService.class);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT );
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient, 3000, pendingIntent);
        Toast.makeText(getApplicationContext(),"Activity Monitoring Started",Toast.LENGTH_SHORT).show();
    }

    public boolean isLocationEnabled(Context context) {
        int locationMode = 0;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);

            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
                return false;
            }

            return locationMode != Settings.Secure.LOCATION_MODE_OFF;

        }else{
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }


    }

    private void startLocMonitoring() {

        Log.d(TAG, "startLocMonitoring Called");
        //Toast.makeText(getApplicationContext(),"Location monitoring started", Toast.LENGTH_SHORT).show();
        try {
            Log.d(TAG,"startLocMonitoring try catch");
            LocationRequest locationRequest = LocationRequest.create()
                    .setInterval(6000)
                    .setFastestInterval(6000)
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG,"startLocMonitoring Permission issues");
                return;
            }
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                    locationRequest, new com.google.android.gms.location.LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            Log.d(TAG,"startLocMonitoring onLocationChanged Called");
                            locationUpdated(location);
                        }
                    });
            Toast.makeText(getApplicationContext(),"Location monitoring started",Toast.LENGTH_SHORT).show();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
