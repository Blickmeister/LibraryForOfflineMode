package cz.fim.uhk.thesis.libraryforofflinemode.model;

import java.util.Date;

public class User {
    private String ssid;

    private double latitude;

    private double longitude;

    private boolean isOnline;

    private String actualState;

    private String futureState;

    private Date firstConnectionToServer;

    private Date lastConnectionToServer;

    private double temperature;

    private double pressure;

    public User(String ssid, double latitude, double longitude, boolean isOnline,
                String actualState, String futureState, Date firstConnectionToServer,
                Date lastConnectionToServer, double temperature, double pressure) {
        this.ssid = ssid;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isOnline = isOnline;
        this.actualState = actualState;
        this.futureState = futureState;
        this.firstConnectionToServer = firstConnectionToServer;
        this.lastConnectionToServer = lastConnectionToServer;
        this.temperature = temperature;
        this.pressure = pressure;
    }

    public String getSsid() {
        return ssid;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public String getActualState() {
        return actualState;
    }

    public String getFutureState() {
        return futureState;
    }

    public Date getFirstConnectionToServer() {
        return firstConnectionToServer;
    }

    public Date getLastConnectionToServer() {
        return lastConnectionToServer;
    }

    public double getTemperature() { return temperature; }

    public double getPressure() { return pressure; }
}
