package com;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DeliveryEstimationService {
    private final ConcurrentHashMap<String, Drone> drones;

    public final ReentrantLock lock = new ReentrantLock();
    public ItemDistributorService itemStorage;
    public double BATTERY_MULTIPLIER = 1;
    public double TIME_MULTIPLIER = 1;
    public Double score;
    public static final double MAX_TIME = 1334.0; 
    public static final double MAX_BATTERY = 37.8; 
    private final double timePerTile = 0.03;
    private final double batteryPerTile = 0.1;
    private final Map<String, Map<Integer, JobEstimate>> estimates = new HashMap<>();
    private final Map<String, Map<Integer, Double>> scoreEstimates = new HashMap<>();
    public record DeliveryEstimate(double time, double battery) {}

    @Autowired
    public DeliveryEstimationService(ConcurrentHashMap<String, Drone> TestDrones, ItemDistributorService itemStorage) {
        this.drones = TestDrones;
        this.itemStorage = itemStorage;

        for (String droneId : drones.keySet()) {
            estimates.put(droneId, new HashMap<>());
            scoreEstimates.put(droneId, new HashMap<>());
        }
    }

    public void removeItemInstance(int itemId) {
        for (Map<Integer, JobEstimate> droneEstimates : estimates.values()) {
            droneEstimates.remove(itemId);
        }
        for (Map<Integer, Double> droneEstimates : scoreEstimates.values()) {
            droneEstimates.remove(itemId);
        }
    }


   public HashMap<Integer, String> getBestScoreForEachItem() {

        
        List<ItemForDelivery> nextItems = new ArrayList<>();
        PriorityBlockingQueue<ItemForDelivery> queue = ItemForDelivery.CurrentItemsAvailable.getItemsForDelivery();
        Iterator<ItemForDelivery> iterator = queue.iterator();

        int availDroneCount = 0;
        for (Drone drone : drones.values()) {
            if (drone.getAvailable().equals(DroneStatus.AVAILABLE.getCode())) {
                availDroneCount++;
            }
        }

        int count = 0;
        while (iterator.hasNext() && count < availDroneCount +3) {
            nextItems.add(iterator.next());
            count++;
        }

        Set<Integer> nextItemIds = new HashSet<>();
        for (ItemForDelivery item : nextItems) {
            nextItemIds.add(item.getItemIdInt());
        }

        Map<String, Map<Integer, Double>> filteredScores = new HashMap<>();
        for (String droneId : scoreEstimates.keySet()) {
            if (!drones.get(droneId).getAvailable().equals(DroneStatus.AVAILABLE.getCode())) {
                continue; 
            }
            Map<Integer, Double> droneScores = scoreEstimates.get(droneId);
            Map<Integer, Double> filtered = new HashMap<>();
            for (Integer itemId : nextItemIds) {
                if (droneScores != null && droneScores.containsKey(itemId)) {
                    filtered.put(itemId, droneScores.get(itemId));
                }
            }
            filteredScores.put(droneId, filtered);
        }

        // Run Hungarian assignment on filtered scores
        HungarianAssignment.Result result = HungarianAssignment.findBestAssignment(filteredScores);

        // Return the itemId -> droneId map
        return new HashMap<>(result.itemToDrone);
    }




     public void updateEstimate(String droneId, int itemId, node destination) {
        ItemForDelivery item = ItemForDelivery.CurrentItemsAvailable.getItemsMap().get(itemId);
        int pathLength = estimatePathLength(droneId, item.getPickUpStation().getLocation(), destination);
        double time = pathLength ;
        double battery = pathLength * batteryPerTile;

        Drone drone = drones.get(droneId);
        if (drone == null) return;

        JobEstimate jobEstimate;
        if (battery > drone.getBattery()) {
            jobEstimate = null;
        } else {
            Location loc = new Location(destination);
            jobEstimate = new JobEstimate(droneId, itemId, loc, time, battery);
        }

        estimates.get(droneId).put(itemId, jobEstimate);
        scoreEstimates.get(droneId).put(itemId, jobEstimate != null ? (double) scoreCreation(time, battery) : 0.0);

        // System.out.println("Path length for item " + itemId + ": Drone " + droneId + " -> " + pathLength);
        // System.out.println(item.getPickUpStation().getLocation());
        // System.out.println(destination);
        // System.out.println(drone.getX() + ", " + drone.getY());
    }



    public DeliveryEstimate calculateEstimate(String droneId, int itemId, Location destination) {
        ItemForDelivery item = ItemForDelivery.CurrentItemsAvailable.getItemsMap().get(itemId);
        int pathLength = estimatePathLength(droneId, item.getPickUpStation().getLocation(), new node((int)destination.getX(), (int)destination.getY()));
        double time = pathLength * timePerTile;
        double battery = pathLength * batteryPerTile;

        Drone drone = drones.get(droneId);
        if (drone == null) return null;

        if (battery > drone.getBattery()) {
            return null;
        }

        return new DeliveryEstimate(time, battery);
    }

    public boolean hasEstimate(String droneId, int itemId) {
        Map<Integer, JobEstimate> droneEstimates = estimates.get(droneId);
        if (droneEstimates == null) {
            return false;
        }
        return droneEstimates.containsKey(itemId);
    }


    private int estimatePathLength(String droneId, node stationLocation, node destination) {
        Drone d = drones.get(droneId);
        if (d == null) return Integer.MAX_VALUE;

        // Distance from drone to pickup station
        double dx1 = d.getX() - stationLocation.getX();
        double dy1 = d.getY() - stationLocation.getY();
        double distanceToStation = Math.sqrt(dx1 * dx1 + dy1 * dy1);

        // Distance from pickup station to destination
        double dx2 = stationLocation.getX() - destination.getX();
        double dy2 = stationLocation.getY() - destination.getY();
        double distanceToDestination = Math.sqrt(dx2 * dx2 + dy2 * dy2);

        return (int) Math.ceil(distanceToStation + distanceToDestination);
    }

    public JobEstimate getEstimate(String droneId, int itemId) {
        Map<Integer, JobEstimate> droneEstimates = estimates.get(droneId);

        if (droneEstimates == null) return null;
        return droneEstimates.get(itemId);
    }

    public Map<String, Map<Integer, Double>> getAllDronesScores() {
        return scoreEstimates;
    }

    public Map<String, Map<Integer, JobEstimate>> getAllDronesEstimates() {
        return estimates;
    }

    public void updateAllEstimates(Map<Integer, Location> itemLocations) {
        lock.lock();
        try {
            for (String droneId : drones.keySet()) {
                Drone d = drones.get(droneId);
                if (!d.getAvailable().equals(DroneStatus.AVAILABLE.getCode())) {
                    continue; 
                }
                for (Map.Entry<Integer, Location> entry : itemLocations.entrySet()) {
                    updateEstimate(droneId, entry.getKey(), new node((int)entry.getValue().getX(), (int)entry.getValue().getY()));
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public void updateAllEstimatesNoParam() {
        lock.lock();
        try {
            for (Drone drone : drones.values()) {
                if (!drone.getAvailable().equals(DroneStatus.AVAILABLE.getCode())) {
                    continue;
                }

                Queue<ItemForDelivery> itemQueue = new LinkedList<>(ItemForDelivery.CurrentItemsAvailable.getItemsForDelivery());
                int count = 0;
                while (!itemQueue.isEmpty() && count < 10) {
                    ItemForDelivery item = itemQueue.poll();
                    updateEstimate(drone.getDroneid(), item.getItemIdInt(), new node((int)item.getTargetLocation().getX(), (int)item.getTargetLocation().getY()));
                    count++;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public double scoreCreation(double time, double battery) {
        
        return ((1 - time / MAX_TIME) );
    }
}
