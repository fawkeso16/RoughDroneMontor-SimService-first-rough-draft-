// Job Class - this class represents a job for a drone, including the drone assigned, the destination, the paths to pick up and destination, and the item being delivered. It provides methods to access and modify these properties.


package com;
import java.util.List;

public class Job {
    
    private String id;
    private Drone drone;
    private Location Destination;
    private List<node> PathToPickUp;
    private List<node> PathToDestination;
    public List<node> fullPath;
    public ItemForDelivery item;   
    public long timeStarted;
    public long timeCompleted; 
    public long duration;


    public Job(String id, ItemForDelivery item, Drone drone, Location destination, List<node> pathToPickUp, List<node> pathToDestination) {
            this.id = id;
            this.drone = drone;
            this.Destination = destination;
            this.PathToPickUp = pathToPickUp;
            this.item = item;
            this.PathToDestination = pathToDestination;
            this.fullPath = new java.util.ArrayList<>();
            this.fullPath.addAll(pathToPickUp);
            this.fullPath.addAll(pathToDestination);
            this.timeStarted = System.currentTimeMillis();
    }


    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public ItemForDelivery getItem() {
        return item;
    }
    public void setItem(ItemForDelivery item) {
        this.item = item;
    }

    public Drone getDrone() {
        return drone;
    }
    public void setDrone(Drone drone) {
        this.drone = drone;
    }
    public Location getDestination() {
        return Destination;
    }
    public void setDestination(Location destination) {
        this.Destination = destination;
    }
    public List<node> getPathToPickUp() {
        return PathToPickUp;
    }
    public void setPathToPickUp(List<node> pathToPickUp) {
        this.PathToPickUp = pathToPickUp;
    }
    public List<node> getPathToDestination() {
        return PathToDestination;
    }
    public void setPathToDestination(List<node> pathToDestination) {
        this.PathToDestination = pathToDestination;
    }
    public List<node> getFullPath() {
        return fullPath;
    }
    public void setFullPath(List<node> fullPath) {
        this.fullPath = fullPath;
    }

    public void appendToFullPath(List<node> extra) {
        if (extra == null || extra.isEmpty()) return;
        this.fullPath.addAll(extra);
    }

    public long getTimeCompleted() {
        return timeCompleted;
    }
    public void setTimeCompleted(long timeCompleted) {
        this.timeCompleted = timeCompleted;
    }
    public long getTimeStarted() {
        return timeStarted;
    }
    public void setTimeStarted(long timeStarted) {
        this.timeStarted = timeStarted;
    }
    public long getJobDuration() {
        return timeCompleted - timeStarted;
    }
    public void setDuration() {
        this.duration = timeCompleted - timeStarted;
    }
    @Override
    public String toString() {
        return "Job{" +
                "id='" + id + '\'' +
                ", drone=" + drone +
                // ", Destination=" + Destination +
                ". Item=" + item + "/n\n" +
                // ", PathToPickUp=" + PathToPickUp +
                // ", PathToDestination=" + PathToDestination +
                ", TimeCompleted=" + timeCompleted +
                '}';
    }

    
}
