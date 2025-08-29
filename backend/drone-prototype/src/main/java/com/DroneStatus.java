// DroneStatus Enum - this enum represents the various statuses a drone can have during its operation, such as available, busy, recharging, pickup, and queueing. Each status has a corresponding code for identification.
package com;

public enum DroneStatus {
    AVAILABLE("Available"),
    BUSY("Busy"),
    RECHARGING("Recharging"),
    PICKUP("Pickup"),
    QUEUEING("Queueing");


    private final String code;

    DroneStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}