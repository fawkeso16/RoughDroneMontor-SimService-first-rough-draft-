package com;

public class JobEstimate {
    private final String droneId;
    private final int itemId;
    private final Location destination;
    private final double estimatedTime;
    private final double estimatedBattery;

    public JobEstimate(String droneId, int itemId, Location destination, double estimatedTime, double estimatedBattery) {
        this.droneId = droneId;
        this.itemId = itemId;
        this.destination = destination;
        this.estimatedTime = estimatedTime;
        this.estimatedBattery = estimatedBattery;
    }

    public String getDroneId() {
        return droneId;
    }

    public int getItemId() {
        return itemId;
    }

    public Location getDestination() {
        return destination;
    }

    public double getEstimatedTime() {
        return estimatedTime;
    }

    public double getEstimatedBattery() {
        return estimatedBattery;
    }

    @Override
    public String toString() {
        return "JobEstimate{" +
               "droneId='" + droneId + '\'' +
               ", itemId=" + itemId +
               ", destination=" + destination +
               ", estimatedTime=" + estimatedTime +
               ", estimatedBattery=" + estimatedBattery +
               '}';
    }

    
}
