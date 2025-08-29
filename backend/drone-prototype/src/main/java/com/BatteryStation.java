package com;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;

public class BatteryStation {
    public String id;
    public String name;
    public node location;
    private Semaphore slots;
    private int totalSlots; 
    
  
    public BatteryStation(String name, node location) {
        this.id = name + "ID";
        this.name = name;
        this.location = location;
        this.totalSlots = 1;
        this.slots = new Semaphore(1);
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
    
    public int getTotalSlots() {
        return totalSlots;
    }

    public void setSlots(int slotCount) {
        this.totalSlots = slotCount;
        this.slots = new Semaphore(slotCount);
    }  

    public void setLocation(node location) {
        this.location = location;
    }
    
    public void addDrone(Drone drone) {
        System.out.println("Drone " + drone.getDroneid() + " attempting to charge at " + name);
        boolean acquired = false;
        try {
            // Try to acquire a slot, waiting up to 2 seconds
            acquired = slots.tryAcquire(2000, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Charging interrupted for drone " + drone.getDroneid());
            return;
        }
        if (acquired) {
            System.out.println("Drone " + drone.getDroneid() + " acquired slot at " + name);
            CompletableFuture.runAsync(() -> this.simulateCharging(drone));
        } else {
            System.out.println("Drone " + drone.getDroneid() + " could not acquire slot at " + name + " (will retry)");
            // Optionally, you can retry after a delay or handle queuing logic here
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(1000); // Wait before retrying
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                addDrone(drone); // Retry
            });
        }
    }

    public void removeDrone(Drone drone) {
        slots.release();

    }

    public boolean isFull() {
        return slots.availablePermits() == 0;
    }

    public boolean isEmpty() {
        return slots.availablePermits() == totalSlots;
    }

    public void simulateCharging(Drone drone) {
        System.out.println("Charging drone " + drone.getDroneid() + " at " + name);
        try {
            Thread.sleep(3000);
            drone.setBattery(100);
            drone.setAvailable(DroneStatus.AVAILABLE.getCode());
            System.out.println("Drone " + drone.getDroneid() + " charged successfully at " + name);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Charging interrupted for drone " + drone.getDroneid());
        } finally {
            this.removeDrone(drone);
            System.out.println("Drone " + drone.getDroneid() + " slot released at " + name);
        }
    }

    public boolean isAvailable() {
        return slots.availablePermits() > 0;
    }

    @Override
    public String toString() {
        return "BatteryStation{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", location=" + location +
                ", availableSlots=" + slots.availablePermits() +
                ", totalSlots=" + totalSlots +
                '}';
    }
}