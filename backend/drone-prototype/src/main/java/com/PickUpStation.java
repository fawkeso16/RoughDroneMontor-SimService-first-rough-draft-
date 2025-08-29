//PickUpStation Class - we use pickupstations to distribute items to drones, we have a semaphore to limit the number of drones that can pick up items at the same time.



package com;
import java.util.concurrent.Semaphore;

public class PickUpStation {
    public String id;
    public String name;
    public node location;
    private final Semaphore slots = new Semaphore(3);
    private final int maxSlots = 3;

    public PickUpStation(String name, node location) {
        this.id = name + "ID";
        this.name = name;
        this.location = location;
    }

    public void requestPickup(Drone drone) {
        System.out.println("Drone " + drone.getDroneid() + " requesting pickup at " + name);
        try {

            if(!slots.tryAcquire()) {
                // System.out.println("No available slots for drone " + drone.getDroneid() + " at " + name);
                drone.setAvailable(DroneStatus.QUEUEING.getCode());
            }      
            else{
                drone.setAvailable(DroneStatus.PICKUP.getCode());
                // System.out.println("Drone " + drone.getDroneid() + " acquired slot at " + name);
            }

            // slots.acquire(); 
            // System.out.println("Drone " + drone.getDroneid() + " starting pickup at " + name + "...");
            Thread.sleep(2000); 
            
            // System.out.println("Drone " + drone.getDroneid() + " completed pickup at " + name + "!");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Pickup interrupted for drone: " + drone.getDroneid());
        } finally {
            drone.setAvailable(DroneStatus.BUSY.getCode());
            slots.release();
            // System.out.println("Drone " + drone.getDroneid() + " released slot. Available: " + slots.availablePermits() + "/3");
        }
    }

    public double getX() {
        return (int)this.location.x;
    }

    public double getY() {
        return (int)this.location.y;
    }

    public node getLocation() {
        return this.location;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }   

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAvailableSlots() {
        return slots.availablePermits();
    }

    public int getUsedSlots() {
        return maxSlots - slots.availablePermits();
    }

    public void setLocation(node location) {
        this.location = location;
    }

    public boolean isAvailable() {
        return slots.availablePermits() > 0;
    }

    public boolean isFull() {
        return slots.availablePermits() <= 0;
    }

    public boolean isEmpty() {
        return slots.availablePermits() >= maxSlots;
    }

    @Override
    public String toString() {
        return "PickUpStation{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", location=" + location +
                ", availableSlots=" + slots.availablePermits() +
                '}';
    }
}