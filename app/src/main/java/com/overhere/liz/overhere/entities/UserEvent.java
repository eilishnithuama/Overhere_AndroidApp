package com.overhere.liz.overhere.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


public class UserEvent {

    @SerializedName("event_id")
    @Expose
    private Integer id;

    @SerializedName("event_type")
    @Expose
    private String event_type;

    @SerializedName("location")
    private GPSLocation lastLocation;

    @SerializedName("timestamp")
    @Expose
    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss.SSS")
    private String timestamp;

    @SerializedName("user_id")
    private int user_id;


    public UserEvent(){

    }

    public UserEvent(Integer id,String event_type, GPSLocation lastLocation,String timestamp,int user_id) {
        this.id = id;
        this.event_type = event_type;
        this.lastLocation = lastLocation;
        this.timestamp = timestamp;
        this.user_id = user_id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEvent_type() {
        return event_type;
    }

    public void setEvent_type(String event_type) {
        this.event_type = event_type;
    }

    public GPSLocation getLastLocation() {
        return lastLocation;
    }

    public void setLastLocation(GPSLocation lastLocation) {
        this.lastLocation = lastLocation;
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
