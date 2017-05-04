package com.overhere.liz.overhere.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Handler;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.telephony.SmsManager;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.overhere.liz.overhere.entities.AndroidUser;
import com.overhere.liz.overhere.entities.UserEvent;
import com.overhere.liz.overhere.http.HttpSender;
import com.overhere.liz.overhere.http.RetrofitInterface;
import com.overhere.liz.overhere.MainActivity;
import com.overhere.liz.overhere.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit.GsonConverterFactory;
import retrofit.Retrofit;

public class GeofenceService extends IntentService {

    public static final String TAG = GeofenceService.class.getSimpleName();
    public static final int alertId = 1234;
    Location lastLocation;
    HttpSender httpSender;

    private Handler handler = new Handler();
    private boolean eventOccurred = false;
    private boolean monitor = true;
    public NotificationManager mNotificationManager;
    private Runnable runnable = new Runnable(){

        @Override
        public void run(){

        if(eventOccurred)
        {
            StatusBarNotification[] sbn = mNotificationManager.getActiveNotifications();
            for(StatusBarNotification s : sbn)
            {
                if(s.getId() == alertId)
                {
                    mNotificationManager.cancel(alertId);
                    sendAlertMessage();
                    UserEvent ue = new UserEvent();
                    DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ENGLISH);
                    String strDate = df.format(new Date());
                    ue.setUser_id(AndroidUser.getInstance().getUser_id());
                    ue.setTimestamp(strDate);
                    ue.setEvent_type("Geofence Exit");
                    ue.setLastLocation(AndroidUser.getInstance().getLastKnownLocation());
                    httpSender.httpSendUserEventInformation(ue);// Send Event to Server
                    eventOccurred = false;
                }
            }
        }
        }
    };

    public GeofenceService() {
        super(TAG);
        Retrofit retrofit = new Retrofit.Builder().baseUrl("https://guarded-falls-61506.herokuapp.com/") //url for web service
                .addConverterFactory(GsonConverterFactory.create()).build();
        RetrofitInterface restInt = retrofit.create(RetrofitInterface.class);
        httpSender = new HttpSender(restInt); //sends http requests to rest service
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);
        if(event.hasError()){
            event.getErrorCode();
        } else {
            int transition = event.getGeofenceTransition();

            if(transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                //Only monitor while inside the geofence
                monitor = true;
            }

            if(transition == Geofence.GEOFENCE_TRANSITION_EXIT){
                Log.d(TAG,"Exit transition called");
                if(monitor){
                    monitor = false;
                    eventOccurred = true;
                    lastLocation = event.getTriggeringLocation();
                    showNotification();
                }
            }
        }
    }

    public void showNotification(){
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.location_icon_1)
                        .setContentTitle("Wandering Detected!")
                        .setContentText("Are you okay? Please clear this notification if you are okay.")
                        .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                        .setVibrate(new long[] { 1000, 1000});

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        mNotificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

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
            handler.postDelayed(runnable,20000); //20 seconds for them to dismiss notification
        }
    }

    public void sendAlertMessage(){
        SmsManager smsManager = SmsManager.getDefault();
        String[] numbers = {};
        String message = "Wandering detected for " + AndroidUser.getInstance().getName();
        message += " their last known location is: \n";
        message += "https://maps.google.com/?q=" + lastLocation.getLatitude() + "," + lastLocation.getLongitude();
        message += ",17z"; // zoom level
        String number = AndroidUser.getInstance().getCarerNumber();
        if(number.contains(",")) {
            numbers = number.split(",");
        }else{
            numbers[0] = number;
        }
        for(String no : numbers) {
            smsManager.sendTextMessage(no, null, message, null, null);
        }
    }
}