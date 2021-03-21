package com.samples.flironecamera;

import java.util.List;

public class Present_student {


    private String ID;
    private String Name;
    private String Status;
    private String Temperature;
    private String Date;
    private String Location;
    private String Time;


    public Present_student() {}

    public Present_student(String ID, String Name, String Status, String Temperature,String Date, String Location, String Time) {
        this.Name=Name;
        this.ID=ID;
        this.Status=Status;
        this.Temperature=Temperature;
        this.Date=Date;
        this.Location=Location;
        this.Time=Time;
    }

    public String getID() {
        return ID;
    }

    public String getname() {
        return Name;
    }

    public String getStatus() {
        return Status;
    }

    public String getTemperature() {
        return Temperature;
    }

    public String getDate() {
        return Date;
    }

    public String getLocation() {
        return Location;
    }

    public String getTime() {
        return Time;
    }


}