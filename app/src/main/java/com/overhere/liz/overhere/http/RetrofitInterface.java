package com.overhere.liz.overhere.http;

import com.overhere.liz.overhere.entities.AndroidUser;
import com.overhere.liz.overhere.entities.Fall;
import com.overhere.liz.overhere.entities.GPSLocation;
import com.overhere.liz.overhere.entities.UserEvent;

import retrofit.Call;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;


public interface RetrofitInterface {

    @POST("/findByUsername")
    Call<AndroidUser> getUser(@Body AndroidUser loginUser);

    @POST("/saveAccel")
    Call<HttpResponseInterface> postWithJson(@Body Fall fall);

    @POST("/saveGPS")
    Call<HttpResponseInterface> postGPSWithJson(@Body GPSLocation location);

    @POST("/saveUserEvent")
    Call<HttpResponseInterface>postUserEventWithJson(@Body UserEvent ue);
}
