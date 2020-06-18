package com.example.aadproject;

public class JoggingSession {
    private String date;
    private String time;
    private String duration; //in minutes:seconds
    private double distance; //in km

    JoggingSession(String date, String time, String duration, double distance){

        this.date = date;
        this.time = time;
        this.duration = duration;
        this.distance = distance;
    }


    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
