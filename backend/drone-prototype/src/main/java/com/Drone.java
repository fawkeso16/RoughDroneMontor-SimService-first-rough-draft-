// Drone Class - this class represents a drone with properties such as ID, coordinates, battery level, availability status, and destination. It includes methods for moving the drone, setting a destination, calculating battery usage, and accessing drone properties.

package com;

import java.util.concurrent.locks.ReentrantLock;

public class Drone {

        public final ReentrantLock lock = new ReentrantLock();

    private String droneid;
    private double x, y;
    private double battery;
    private String available;
    private Location destination;

    public Drone(String droneid, double x, double y, double battery) {
        this.droneid = droneid;
        this.x = x;
        this.y = y;
        this.battery = battery;

        this.available = DroneStatus.AVAILABLE.getCode();
        this.destination = null; 
    }

    public void moveTo(double newX, double newY) {
        this.x = newX;
        this.y = newY;
    }

    public void setDestination(Location destination) {
        this.destination = destination;
    }

    public double calculateBatteryUsage(double distance){
        // Use integer arithmetic to avoid floating point precision issues
        int batteryUsage = (int)(distance * 10); // Multiply by 10, then divide by 100
        return batteryUsage / 100.0;
    }

    public Location getDestination() {
        return destination;
    }

    public String getDroneid() {
        return droneid;
    }
    public void setDroneid(String droneid) {
        this.droneid = droneid;
    }
    public double getX() {
        return x;
    }
    public void setX(double x) {
        this.x = x;
    }
    public double getY() {
        return y;
    }
    public void setY(double y) {
        this.y = y;
    }
    public double getBattery() {
        return battery;
    }
    public void setBattery(double battery) {
        this.battery = battery;
    }
    public String getAvailable() {
        return available;
    }
    public void setAvailable(String available) {
        this.available = available;
    }

    @Override
    public String toString() {
        return "Drone{" +
                "droneid='" + droneid + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", battery=" + battery +
                ", available=" + available +
                '}';
    }


}
