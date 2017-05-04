package com.overhere.liz.overhere;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;

public class SplashScreen extends AppCompatActivity {

    private static final boolean AUTO_HIDE = true;

    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    private static int SPLASH_TIMEOUT = 3000;

    private boolean mVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash_screen);

        mVisible = true;


        new Handler().postDelayed(new Runnable() {


            @Override
            public void run() {
                SharedPreferences pref = getApplicationContext().getSharedPreferences("PREFERENCES", MODE_PRIVATE);
                //Hasn't logged in before
                System.out.println("SPLASH: "+pref.getString("username", null));
                if(pref.getString("username", null) == null){
                    Intent i = new Intent(SplashScreen.this, LoginActivity.class);
                    startActivity(i);
                }
                else{
                    //Has logged in before
                    Intent i = new Intent(SplashScreen.this, MainActivity.class);
                    startActivity(i);

                }

                // close the splash screen
                finish();
            }
        }, SPLASH_TIMEOUT);
        toggle();

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mVisible = false;

    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mVisible = true;

    }

}
