package com.overhere.liz.overhere.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Fall {

    @SerializedName("id")
    @Expose
    private Integer id;

    @SerializedName("xaxis")
    @Expose
    private Double xaxis;

    @SerializedName("yaxis")
    @Expose
    private Double yaxis;
    @SerializedName("zaxis")
    @Expose
    private Double zaxis;

    @SerializedName("userid")
    @Expose
    @JsonFormat(shape=JsonFormat.Shape.NUMBER_INT)
    private Integer userid;

    @SerializedName("timestamp")
    @Expose
    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss.SSS")
    private String timestamp;

    public Fall(){

    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }


    public Double getXaxis() {
        return xaxis;
    }

    public void setXaxis(Double xaxis) {
        this.xaxis = xaxis;
    }

    public Double getYaxis() {
        return yaxis;
    }

    public void setYaxis(Double yaxis) {
        this.yaxis = yaxis;
    }

    public Double getZaxis() {
        return zaxis;
    }

    public void setZaxis(Double zaxis) {
        this.zaxis = zaxis;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getUserId(){
        return this.userid;
    }

    public void setUserId( Integer user_id){
        this.userid = user_id;
    }

}
