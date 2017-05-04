package com.overhere.liz.overhere.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class GPSLocation {

    private Integer id;
    private float longitude;
    private float latitude;

    @SerializedName("user_id")
    @Expose
    @JsonFormat(shape=JsonFormat.Shape.NUMBER_INT)
    private Integer user_id;

    @SerializedName("timestamp")
    @Expose
    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss.SSS")
    private String timestamp;

    public GPSLocation(Integer id, float longitude, float latitude, int user_id, String timestamp) {
        this.id = id;
        this.longitude = longitude;
        this.latitude = latitude;
        this.user_id = user_id;
        this.timestamp = timestamp;
    }

    public GPSLocation() {

    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

}
