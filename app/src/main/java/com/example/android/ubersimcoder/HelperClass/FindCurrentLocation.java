package com.example.android.ubersimcoder.HelperClass;

public class FindCurrentLocation {

    public String id;
    public double latitude;
    public double longitude;

    public FindCurrentLocation(){
        // Default Constructor required for calls to DaraSnapshot.getValue(DrivaerCurrentLocation.class)

    }

    public FindCurrentLocation(String id, double latitude, double longitude){
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
