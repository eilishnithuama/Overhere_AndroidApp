package com.overhere.liz.overhere.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.overhere.liz.overhere.controller.AppController;
import com.overhere.liz.overhere.entities.AndroidUser;
import com.overhere.liz.overhere.entities.Fall;
import com.overhere.liz.overhere.entities.UserEvent;
import com.overhere.liz.overhere.http.HttpSender;
import com.overhere.liz.overhere.http.RetrofitInterface;
import com.overhere.liz.overhere.MainActivity;
import com.overhere.liz.overhere.R;

import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit.GsonConverterFactory;
import retrofit.Retrofit;

public class ActivityRecognitionService extends IntentService implements SensorEventListener {

    public static final String TAG = ActivityRecognitionService.class.getSimpleName();
    public static final int alertId = 1236;
    private HttpSender httpSender;
    private Fall fall;
    private float MAX_ACCEL_VALUE;
    private Handler handler = new Handler();
    private boolean monitor = true;
    private int currentActivity;
    private int count;
    private double maxReading;
    private double previousAccel;
    private boolean eventOccurred = false;
    private NotificationManager mNotificationManager;
    private Handler sensorHandler = new Handler();
    private static boolean isIntentServiceRunning;

    private Runnable runnable = new Runnable(){

        @Override
        public void run(){
            if(eventOccurred)
            {
                mNotificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
                StatusBarNotification[] sbn = mNotificationManager.getActiveNotifications();
                Log.d(TAG,"Event detected!");
                for(StatusBarNotification s : sbn)
                {
                    if(s.getId() == alertId) {
                        //Dismiss the active notification
                        mNotificationManager.cancel(alertId);
                        //sendAlertMessage();
                        UserEvent ue = new UserEvent();
                        ue.setUser_id(AndroidUser.getInstance().getUser_id());
                        ue.setTimestamp(fall.getTimestamp());
                        ue.setEvent_type("Fall");
                        ue.setLastLocation(AndroidUser.getInstance().getLastKnownLocation());
                        httpSender.httpSendUserEventInformation(ue);


                    }
                }
                Log.d(TAG,"Monitoring turned back on");
                //Go back to monitoring
                eventOccurred = false;
                monitor = true;
            }
        }
    };

    private Runnable sensorRunnable = new Runnable() {

        @Override
        public void run() {
            Log.d(TAG,"current activity :" + currentActivity);

            if(monitor && (currentActivity != DetectedActivity.IN_VEHICLE &&
                    currentActivity != DetectedActivity.RUNNING &&
                    currentActivity != DetectedActivity.ON_BICYCLE)) {
                Log.d(TAG,"Evaluating readings");
                fallCalculations();
            }
            sensorHandler.postDelayed(this,500);
        }
    };

    public ActivityRecognitionService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d(TAG,"handle intent activity recognition");
        if(!isIntentServiceRunning) {
            Log.d(TAG,"intial run");
            //Start Accelerometer Readings
            SensorManager sensorManager = (SensorManager)getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
            fall = new Fall();
            if (sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).size() != 0) {
                Sensor accelSensor = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
                sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
            count = 0;

            Retrofit retrofit = new Retrofit.Builder().baseUrl(getString(R.string.webUrl))
                    .addConverterFactory(GsonConverterFactory.create()).build();
            RetrofitInterface restInt = retrofit.create(RetrofitInterface.class);
            httpSender = new HttpSender(restInt); //sends http requests to rest service

            SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("PREFERENCES", MODE_PRIVATE);
            MAX_ACCEL_VALUE = sharedPref.getFloat("MAX_ACCEL_VALUE", 10);
            //maxReading will increase with each false positive reading
            // making the detection more accurate each time
            maxReading = MAX_ACCEL_VALUE;


            //Start sensor monitoring
            sensorHandler.postDelayed(sensorRunnable,5000);

            if(ActivityRecognitionResult.hasResult(intent)) {
                ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
                handleDetectedActivities( result.getProbableActivities() );
            }
        }
        else{
            if(ActivityRecognitionResult.hasResult(intent)) {
                Log.d(TAG,"AR API has result");
                ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
                handleDetectedActivities( result.getProbableActivities() );
            }
        }
        isIntentServiceRunning = true;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        fall.setXaxis((double) sensorEvent.values[0]);
        fall.setYaxis((double) sensorEvent.values[1]);
        fall.setZaxis((double) sensorEvent.values[2]);
        fall.setUserId(AndroidUser.getInstance().getUser_id());
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void fallCalculations(){
        if(fall != null) {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ENGLISH);
            String strDate = df.format(new Date());
            fall.setTimestamp(strDate);
            double gravity = 9.8; //estimate;
            double acceleration = Math.sqrt(fall.getXaxis() * fall.getXaxis() +
                    fall.getYaxis() * fall.getYaxis() +
                    fall.getZaxis() + fall.getZaxis()) - gravity;

            double expectedValue;
            if(previousAccel < acceleration)
                expectedValue = acceleration - previousAccel;
            else
                expectedValue = previousAccel - acceleration;

            double roll = Math.asin(fall.getXaxis() / gravity);
            double pitch = Math.asin(fall.getZaxis() / gravity);

            Log.d(TAG,"Previous Reading: " + String.valueOf(previousAccel));
            Log.d(TAG,"Current Reading: " + String.valueOf(acceleration));
            Log.d(TAG,"Expected Value: " + String.valueOf(expectedValue));
            Log.d(TAG,"Current Roll: " + String.valueOf(roll));
            Log.d(TAG,"Current Pitch: " + String.valueOf(pitch));

            previousAccel = acceleration;

            if (expectedValue > MAX_ACCEL_VALUE)
            {
                //Stop the monitoring and send the notification
                Log.e(TAG,"Fall detected");
                monitor = false;
                eventOccurred = true;
                //maxReading = acceleration;
                previousAccel = 0; // reset so alert doesnt go off again immediately after
                showNotification(roll,pitch);
                SharedPreferences pref = getApplicationContext().getSharedPreferences("PREFERENCES", MODE_PRIVATE);
                SharedPreferences.Editor editor = pref.edit();
                editor.putFloat("MAX_ACCEL_VALUE",(float)maxReading);
            }

            httpSender.httpSendFallInformation(fall);
        }
    }

    private void handleDetectedActivities(List<DetectedActivity> probableActivities) {
        for( DetectedActivity activity : probableActivities ) {
            switch( activity.getType() ) {
                case DetectedActivity.IN_VEHICLE: {
                    Log.i( "ActivityRecogition", "In Vehicle: " + activity.getConfidence() );
                    if( activity.getConfidence() >= 75 ) {
                        monitor = false;
                        currentActivity = DetectedActivity.IN_VEHICLE;
                        AppController.getInstance(getApplicationContext()).deregisterGeofence();
                    }
                    if(activity.getConfidence() <= 75){
                        monitor = true;
                    }
                    break;
                }
                case DetectedActivity.ON_BICYCLE: {
                    if( activity.getConfidence() >= 75 ) {
                        monitor = false;
                        currentActivity = DetectedActivity.ON_BICYCLE;
                        AppController.getInstance(getApplicationContext()).deregisterGeofence();
                    }
                    Log.i( "ActivityRecogition", "On Bicycle: " + activity.getConfidence() );
                    break;
                }
                case DetectedActivity.ON_FOOT: {
                    if( activity.getConfidence() >= 75 ) {
                        monitor = true;
                        currentActivity = DetectedActivity.ON_FOOT;
                        AppController.getInstance(getApplicationContext()).reRegisterGeofence();
                    }

                    Log.i( "ActivityRecogition", "On Foot: " + activity.getConfidence() );
                    break;
                }
                case DetectedActivity.RUNNING: {
                    if( activity.getConfidence() >= 75 ) {
                        monitor = false;
                        currentActivity = DetectedActivity.RUNNING;
                        AppController.getInstance(getApplicationContext()).deregisterGeofence();
                    }
                    Log.i( "ActivityRecogition", "Running: " + activity.getConfidence() );
                    break;
                }
                case DetectedActivity.STILL: {
                    if( activity.getConfidence() >= 75 ) {
                        monitor = true;
                        currentActivity = DetectedActivity.STILL;
                        AppController.getInstance(getApplicationContext()).reRegisterGeofence();
                    }
                    Log.i( "ActivityRecogition", "Still: " + activity.getConfidence() );
                    break;
                }
                case DetectedActivity.TILTING: {
                    if( activity.getConfidence() >= 75 ) {
                        monitor = true;
                        currentActivity = DetectedActivity.TILTING;
                        AppController.getInstance(getApplicationContext()).reRegisterGeofence();
                    }
                    Log.i( "ActivityRecogition", "Tilting: " + activity.getConfidence() );
                    break;
                }
                case DetectedActivity.WALKING: {
                    if( activity.getConfidence() >= 75 ) {
                        monitor = true;
                        currentActivity = DetectedActivity.WALKING;
                        AppController.getInstance(getApplicationContext()).reRegisterGeofence();
                    }
                    Log.i( "ActivityRecogition", "Walking: " + activity.getConfidence() );

                    break;
                }
                case DetectedActivity.UNKNOWN: {
                    if( activity.getConfidence() >= 75 ) {
                        monitor = true;
                        currentActivity = DetectedActivity.UNKNOWN;
                        AppController.getInstance(getApplicationContext()).reRegisterGeofence();
                    }
                    Log.i( "ActivityRecogition", "Unknown: " + activity.getConfidence() );
                    break;
                }
            }
        }
    }

    public void showNotification(double roll,double pitch) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.location_icon_1)
                        .setContentTitle("Are you okay?")
                        .setContentText(
                                "Please swip this notification away if you are okay!"
                                        + "Roll: " + roll + " Pitch: " + pitch)
                        .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                        .setVibrate(new long[]{1000, 1000});

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        //Check if the notification is already shown
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        StatusBarNotification[] sbn = mNotificationManager.getActiveNotifications();
        boolean exists = false;
        for (StatusBarNotification s : sbn) {
            if (s.getId() == alertId) {
                exists = true;
            }
        }
        if(!exists) {
            mNotificationManager.notify(alertId, mBuilder.build());
            handler.postDelayed(runnable,20000); //30 seconds for them to dismiss notification
        }
    }

    public void sendAlertMessage() {
        SmsManager smsManager = SmsManager.getDefault();
        String[] numbers = {};
        String message = " A Fall has been detected for " + AndroidUser.getInstance().getName();
        message += ". They may need help! Their last known location is: \n";
        message += "https://maps.google.com/?q=" + AndroidUser.getInstance().getLastKnownLocation().getLatitude()
                + "," + AndroidUser.getInstance().getLastKnownLocation().getLongitude();
        message += ",19z"; // zoom level
        String number = AndroidUser.getInstance().getCarerNumber();
        if (number.contains(",")) {
            numbers = number.split(",");
        } else {
            numbers[0] = number;
        }
        for (String no : numbers) {
            smsManager.sendTextMessage(no, null, message, null, null);
        }
    }

    public boolean getIsIntentServiceRunning(){
        return isIntentServiceRunning;
    }
}
