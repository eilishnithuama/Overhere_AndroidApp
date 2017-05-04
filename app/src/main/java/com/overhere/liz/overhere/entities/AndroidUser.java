package com.overhere.liz.overhere.entities;

public class AndroidUser {

    private int user_id;
    private String name;
    private String username;
    private String password;
    private String carerNumber;
    private GPSLocation homeAddress;
    private GPSLocation lastKnownLocation;

    private static AndroidUser instance = null;

    protected AndroidUser() {
    }

    public static AndroidUser getInstance() {
        if(instance == null) {
            instance = new AndroidUser();
        }

        return instance;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public GPSLocation getHomeAddress() {
        return homeAddress;
    }

    public void setHomeAddress(GPSLocation homeAddress) {
        this.homeAddress = homeAddress;
    }

    public GPSLocation getLastKnownLocation(){
        return lastKnownLocation;
    }

    public void setLastKnownLocation(GPSLocation location){
        this.lastKnownLocation = location;
    }

    public String getCarerNumber() {
        return carerNumber;
    }

    public void setCarerNumber(String carerNumber){
        this.carerNumber = carerNumber;
    }

    @Override
    public String toString() {
        return "AndroidUser{" +
                "user_id=" + user_id +
                ", name='" + name + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", carerNumber='" + carerNumber + '\'' +
                ", homeAddress=" + homeAddress +
                '}';
    }
}
