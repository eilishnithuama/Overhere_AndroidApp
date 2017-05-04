package com.overhere.liz.overhere.entities;

/**
 * Created by eilis on 21/03/2017.
 */

import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class AccelFallEvent {

    @SerializedName("id")
    @Expose
    private int id;

    @SerializedName("acceleration")
    @Expose
    private Double acceleration;

    @SerializedName("roll")
    @Expose
    private Double roll;

    @SerializedName("pitch")
    @Expose
    private Double pitch;

    @SerializedName("response")
    @Expose
    private String response;

    @SerializedName("timestamp")
    @Expose
    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss.SSS")
    private String timestamp;


    public Double getAcceleration() {
        return acceleration;
    }

    public void setAcceleration(Double acceleration) {
        this.acceleration = acceleration;
    }

    public AccelFallEvent withAcceleration(Double accleration) {
        this.acceleration = acceleration;
        return this;
    }

    public Double getRoll() {
        return roll;
    }

    public void setRoll(Double roll) {
        this.roll = roll;
    }

    public AccelFallEvent withRoll(Double roll) {
        this.roll = roll;
        return this;
    }

    public Double getPitch() {
        return pitch;
    }

    public void setPitch(Double pitch) {
        this.pitch = pitch;
    }

    public AccelFallEvent withPitch(Double pitch) {
        this.pitch = pitch;
        return this;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public AccelFallEvent withResponse(String response) {
        this.response = response;
        return this;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTimeStamp() {
        return timestamp;
    }

    public void setTimeStamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public AccelFallEvent withTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }
}
