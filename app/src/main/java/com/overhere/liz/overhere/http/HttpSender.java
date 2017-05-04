package com.overhere.liz.overhere.http;

import android.util.Log;

import com.overhere.liz.overhere.entities.AndroidUser;
import com.overhere.liz.overhere.entities.Fall;
import com.overhere.liz.overhere.entities.GPSLocation;
import com.overhere.liz.overhere.entities.UserEvent;

import java.io.IOException;

import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

public class HttpSender {

    private RetrofitInterface restInt;
    private static final String TAG = "HTTP SEND: ";

    public HttpSender(RetrofitInterface restInt) {
        this.restInt = restInt;

    }

    public void getUser(AndroidUser au) {

        Log.d("HTTP SEND: ", "Finding User");
        Call<AndroidUser> call = restInt.getUser(au);
        // Asynchronously execute HTTP request
        call.enqueue(new Callback<AndroidUser>() {
            /**
             * onResponse is called when any kind of response has been received.
             */
            @Override
            public void onResponse(Response<AndroidUser> response, Retrofit retrofit) {
                // http response status code + headers
                System.out.println("Response status code: " + response.code());
                if (!response.isSuccess()) {
                    try {
                        System.out.println(response.errorBody().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return;
                }

                Log.d("HTTP SEND: ",response.body().toString());


                AndroidUser.getInstance().setName(response.body().getName());
                AndroidUser.getInstance().setPassword(response.body().getPassword());
                AndroidUser.getInstance().setUser_id(response.body().getUser_id());
                AndroidUser.getInstance().setCarerNumber(response.body().getCarerNumber());
                AndroidUser.getInstance().setHomeAddress(response.body().getHomeAddress());

            }

            @Override
            public void onFailure(Throwable t) {
                System.out.println("onFailure");
                System.out.println(t.getMessage());
            }
        });

    }

    public void httpSendFallInformation(Fall accelFall) {

        Log.d("HTTP SEND: ", "fall information being sent");
        Call<HttpResponseInterface> call = restInt.postWithJson(accelFall);
        call.enqueue(new Callback<HttpResponseInterface>() {

            @Override
            public void onResponse(Response<HttpResponseInterface> response, Retrofit retrofit) {
                // http response status code + headers
                System.out.println("Response status code: " + response.code());
                if (!response.isSuccess()) {
                    try {
                        System.out.println(response.errorBody().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return;
                }
                HttpResponseInterface decodedResponse = response.body();
                if (decodedResponse == null) return;
                /*System.out.println("Response (contains request infos):");
                System.out.println("- url:         " + decodedResponse.url);
                System.out.println("- ip:          " + decodedResponse.origin);
                System.out.println("- headers:     " + decodedResponse.headers);
                System.out.println("- args:        " + decodedResponse.args);
                System.out.println("- form params: " + decodedResponse.form);
                System.out.println("- json params: " + decodedResponse.json);*/
            }

            @Override
            public void onFailure(Throwable t) {
                System.out.println("onFailure");
                System.out.println(t.getMessage());
            }
        });
    }

    public void httpSendGPSInformation(GPSLocation location) {

        Log.d("HTTP SEND: ", "GPS information being sent");
        Call<HttpResponseInterface> call = restInt.postGPSWithJson(location);
        // Asynchronously execute HTTP request
        call.enqueue(new Callback<HttpResponseInterface>() {
            @Override
            public void onResponse(Response<HttpResponseInterface> response, Retrofit retrofit){
                // http response status code + headers
                Log.d(TAG, "Response Code: " + response.code());
                if (!response.isSuccess()) {
                    try {
                        System.out.println(response.errorBody().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return;
                }
                // if parsing the JSON body failed, `response.body()` returns null
                HttpResponseInterface decodedResponse = response.body();
                if (decodedResponse == null) return;
                else Log.i(TAG,decodedResponse.toString());
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG,"Send Failure");
                System.out.println(t.getStackTrace());
            }
        });
    }



    public void httpSendUserEventInformation(UserEvent ue) {

        Log.d(TAG, "UserEvent");
        Call<HttpResponseInterface> call = restInt.postUserEventWithJson(ue);
        // Asynchronously execute HTTP request
        call.enqueue(new Callback<HttpResponseInterface>() {
            @Override
            public void onResponse(Response<HttpResponseInterface> response, Retrofit retrofit) {
                // http response status code + headers
                Log.d(TAG,"Response status code: " + response.code());
                if (!response.isSuccess()) {
                    try {
                        System.out.println(response.errorBody().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return;
                }

                // if parsing the JSON body failed, `response.body()` returns null
                HttpResponseInterface decodedResponse = response.body();
                if (decodedResponse == null) return;

            }

            @Override
            public void onFailure(Throwable t) {
                System.out.println("onFailure");
                System.out.println(t.getMessage());
            }
        });
    }
}
