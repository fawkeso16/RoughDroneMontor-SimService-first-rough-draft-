package com;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;



@Service    
public class TestEnvironment {
    public final ReentrantLock lock = new ReentrantLock();

    private final DeliveryEstimationService deliveryEstimationService;
    private final LogManager logManager;
    private final ItemDistributorService itemDistributorService;
    private final TestDroneService droneService;
    private final service jobs;
    private final List<Location> testTargets;
    public int itemCount = 0;
    private final ConcurrentHashMap<String, Drone> drones;  
    private final List<PickUpStation> stations;
    private final HashMap<String, java.util.Map<node, PickUpStation>> items = new HashMap<>();
    public record DeliveryEstimate(double time, double battery) {}
    private final AtomicInteger dualJobCount = new AtomicInteger(0);
    

    private static class ScheduledItem {
        final long scheduledTime;
        final int count;
        final boolean priority;
        boolean added = false;
        
        ScheduledItem(long time, int count, boolean priority) {
            this.scheduledTime = time;
            this.count = count;
            this.priority = priority;
        }
    }
   
    private int locationIndex = 0;

    private synchronized Location getNextLocation() {
        Location loc = testTargets.get(locationIndex);
        locationIndex = (locationIndex + 1) % testTargets.size();
        return loc;
    }
        


    @Autowired
    public TestEnvironment(ConcurrentHashMap<String, Drone> TestDrones, service jobs,
                        List<Location> TestTargets, List<BatteryStation> TestBatteryStations,
                        List<PickUpStation> TestPickUpStations, LogManager logManager,
                        ItemDistributorService itemDistributorService,
                        TestDroneService TestDroneService,
                        DeliveryEstimationService deliveryEstimationService) {
        this.droneService = TestDroneService;
        this.drones = TestDrones;
        this.itemDistributorService = itemDistributorService;
        this.logManager = logManager;
        this.jobs = jobs;
        this.testTargets = TestTargets;
        this.deliveryEstimationService = deliveryEstimationService;
        this.stations = TestPickUpStations;
}



    public void addHardcodedItems(int amount, boolean priority) {
        items.clear(); 
        PickUpStation[] stationArr = new PickUpStation[4];
        for (PickUpStation station : stations) {
            node loc = station.getLocation();
            if (loc.x == 25 && loc.y == 40) {
                stationArr[0] = station;
            } else if (loc.x == 66 && loc.y == 130) {
                stationArr[1] = station;
            } else if (loc.x == 145 && loc.y == 145) {
                stationArr[2] = station;
            } else if (loc.x == 140 && loc.y == 60) {
                stationArr[3] = station;
            }
        }

        for (int i = 0; i < 4; i++) {
            if (stationArr[i] == null) {
                throw new IllegalStateException("Could not find all required pickup stations for this test.");
            }
        }

        if (priority) {
            Location destination = getNextLocation();
            ItemForDelivery item = new ItemForDelivery("PriorityItem" + itemCount, "HighPriorityItem", stationArr[0], true, new node((int)destination.getX(), (int)destination.getY()));
            itemCount++;
            itemDistributorService.addPriorityItem(item);
            return;
        }
        
        for (int i = 0; i < amount; i++) {
            Location destination = getNextLocation();

            int stationIdx = i % 4;
            String itemId = "Item" + itemCount;
            java.util.Map<node, PickUpStation> stationMap = new HashMap<>();
            stationMap.put(new node ((int)destination.getX(), (int)destination.getY()), stationArr[stationIdx]);
            items.put(itemId, stationMap);
            itemCount++;
        }


        itemDistributorService.addDefinedItems(items);
    }


    // Basic quick test: one available drone, two items sharing the FIRST pickup station in list, two destinations.
    public CompletableFuture<Boolean> testSingleDroneDualItem() {
        Drone drone = drones.values().stream()
            .filter(d -> d.getAvailable().equals(DroneStatus.AVAILABLE.getCode()))
            .findFirst().orElse(null);
        if (drone == null) {
            System.out.println("No available drone for dual test");
            return CompletableFuture.completedFuture(false);
        }

        if (stations.isEmpty()) {
            System.out.println("No pickup stations configured");
            return CompletableFuture.completedFuture(false);
        }
        PickUpStation station = stations.get(0);

        if (testTargets.size() < 2) {
            System.out.println("Not enough targets for dual test");
            return CompletableFuture.completedFuture(false);
        }
        Location dest1 = testTargets.get(0);
        Location dest2 = testTargets.get(1);

        ItemForDelivery itemA = new ItemForDelivery("DualTestItemA", "DualA", station, new node((int)dest1.getX(), (int)dest1.getY()));
        ItemForDelivery itemB = new ItemForDelivery("DualTestItemB", "DualB", station, new node((int)dest2.getX(), (int)dest2.getY()));
        ItemForDelivery.CurrentItemsAvailable.addItem(itemA);
        ItemForDelivery.CurrentItemsAvailable.addItem(itemB);

        System.out.println("Starting dual job test for drone " + drone.getDroneid());
        return droneService.createJobDouble(drone, itemA, dest1, itemB, dest2)
            .whenComplete((res, ex) -> {
                if (ex != null) {
                    System.out.println("Dual job test exception: " + ex.getMessage());
                } else {
                    System.out.println("Dual job test result: " + res);
                }
            });
    }



    public void DeterministicSchedulerTest() {
        DeterministicItemScheduler scheduler = new DeterministicItemScheduler();
        scheduler.scheduleItems();
        
        long startTime = System.currentTimeMillis();
        List<CompletableFuture<Boolean>> jobFutures = new ArrayList<>();
        
        while (System.currentTimeMillis() - startTime < 60000) { 
            long currentTime = System.currentTimeMillis() - startTime;
            
            List<ScheduledItem> newItems = scheduler.getItemsAtTime(currentTime);
            for (ScheduledItem scheduledItem : newItems) {
                addHardcodedItems(scheduledItem.count, scheduledItem.priority);
            }
            
            List<CompletableFuture<Boolean>> newJobs = processAvailableItems();
            jobFutures.addAll(newJobs);
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

    
        CompletableFuture<Void> allJobs = CompletableFuture.allOf(jobFutures.toArray(new CompletableFuture[0]));
        
        try {
            allJobs.join();
            
            long successfulJobs = jobFutures.stream()
                .mapToLong(future -> {
                    try {
                        return future.get() ? 1L : 0L;
                    } catch (Exception e) {
                        return 0L;
                    }
                })
                .sum();
            
            System.out.println("All jobs completed. Successful: " + successfulJobs + "/" + jobFutures.size());
            
        } catch (Exception e) {
            System.err.println("Error waiting for jobs to complete: " + e.getMessage());
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        TestReport(duration);
    }


    public List<CompletableFuture<Boolean>> processAvailableItems() {
        PriorityBlockingQueue<ItemForDelivery> currentItems = ItemForDelivery.CurrentItemsAvailable.getItemsForDelivery();
        List<CompletableFuture<Boolean>> jobFutures = new ArrayList<>();
        
        while (true) {
            ItemForDelivery item = currentItems.peek();  
            if (item == null) break;  
            
            Drone closestDrone = droneService.findClosestAvailableDrone(item.getPickUpStation().getLocation());
            if (closestDrone == null) {
                
                continue;
            }
            
            item = currentItems.poll();
            if (item == null) break; 

            ItemForDelivery second = findMergeCandidate(item, currentItems);
            try {
                if (second != null) {
                    boolean removed = currentItems.remove(second); 
                    if (!removed) {
                        second = null;
                    }
                }

                CompletableFuture<Boolean> jobFuture;
                if (second != null) {
                    dualJobCount.incrementAndGet();
                    String[] logMsg = { ItemDistributorService.getHourWithAmPm(), "MERGE", "Double job: " + item.getItemId() + "+" + second.getItemId() };
                    logManager.addLog(logMsg);
                    jobFuture = droneService.createJobDouble(
                        closestDrone,
                        item, new Location(item.getTargetLocation()),
                        second, new Location(second.getTargetLocation())
                    );
                } else {
                    jobFuture = droneService.createJob(closestDrone, item, new Location(item.getTargetLocation()));
                }
                jobFutures.add(jobFuture);
            } catch (Exception e) {
                ItemForDelivery.CurrentItemsAvailable.addItem(item);
                if (second != null) {
                    ItemForDelivery.CurrentItemsAvailable.addItem(second);
                }
            }
        }
        
        return jobFutures;
    }

    // Find a second item with same pickup station and Manhattan distance <= 50 between destinations
    private ItemForDelivery findMergeCandidate(ItemForDelivery first, PriorityBlockingQueue<ItemForDelivery> queue) {
        PickUpStation pickup = first.getPickUpStation();
        node firstDest = first.getTargetLocation();
        for (ItemForDelivery candidate : queue) { 
            if (candidate == first) continue; 
            if (candidate.getPickUpStation() != pickup) continue; 
            node candDest = candidate.getTargetLocation();
            int dist = manhattan(firstDest, candDest);
            if (dist <= 50) {
                return candidate;
            }
        }
        return null;
    }

    private int manhattan(node a, node b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }

    private String coords(node n) { return "(" + n.x + "," + n.y + ")"; }


    List<JobEstimate> generateAllJobEstimates(Collection<Drone> drones, Collection<ItemForDelivery> items) {
        List<JobEstimate> jobEstimates = new ArrayList<>();

        for (var drone : drones) {
            for (ItemForDelivery item : items) {
                Location loc = getNextLocation();
                DeliveryEstimationService.DeliveryEstimate estimate = deliveryEstimationService.calculateEstimate(drone.getDroneid(), item.getItemIdInt(), loc);
                jobEstimates.add(new JobEstimate(drone.getDroneid(), item.getItemIdInt(), loc, estimate.time(), estimate.battery()));
                deliveryEstimationService.updateEstimate(drone.getDroneid(), item.getItemIdInt(), new node((int)loc.getX(), (int)loc.getY()));
                // Double score = deliveryEstimationService.scoreEstimates.get(drone.getDroneid()).get(item.getItemIdInt());
                
            }
        }

        return jobEstimates;
    }


    //HUNGARIAN
    public void TestAllEstimates() throws InterruptedException {
        DeterministicItemScheduler scheduler = new DeterministicItemScheduler();
        scheduler.scheduleItems();

        long startTime = System.currentTimeMillis();
        List<CompletableFuture<Boolean>> jobFutures = new ArrayList<>();
        long lastEstimateTime = startTime;

        

        while (System.currentTimeMillis() - startTime < 60000 || !ItemForDelivery.CurrentItemsAvailable.isEmpty()) {
            int activeDroneCount = 0;
            for (Drone drone : drones.values()) {
                if (drone.getAvailable().equals(DroneStatus.AVAILABLE.getCode())) {
                    activeDroneCount++;
                }
            }

            long currentTime = System.currentTimeMillis() - startTime;

            List<ScheduledItem> newItems = scheduler.getItemsAtTime(currentTime);
            for (ScheduledItem scheduledItem : newItems) {
                addHardcodedItems(scheduledItem.count, scheduledItem.priority);
            }

            if (System.currentTimeMillis() - lastEstimateTime >= 3000 || activeDroneCount > 5) {
                lastEstimateTime = System.currentTimeMillis();

                deliveryEstimationService.updateAllEstimatesNoParam();

                HashMap<Integer, String> bestAssignments = deliveryEstimationService.getBestScoreForEachItem();
                for (java.util.Map.Entry<Integer, String> entry : bestAssignments.entrySet()) {
                    int itemId = entry.getKey();
                    String droneId = entry.getValue();

                    ItemForDelivery item = ItemForDelivery.CurrentItemsAvailable.getItemsMap().get(itemId);
                    Drone drone = drones.get(droneId);

                    if (item != null && drone != null && drone.getAvailable().equals(DroneStatus.AVAILABLE.getCode())) {
                        CompletableFuture<Boolean> jobFuture = droneService.createJob(drone, item, new Location(item.getTargetLocation()));
                        deliveryEstimationService.removeItemInstance(itemId);
                        jobFutures.add(jobFuture);
                        System.out.println("Assigned Item " + itemId + " to Drone " + droneId);
                    }
                }

                for(Drone drone : drones.values()) {
                    if (drone.getAvailable().equals(DroneStatus.AVAILABLE.getCode()) && drone.getBattery() < 20) {
                        droneService.sendToNearestBatteryStation(drone);

                    }
                }
                
            }

        
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

    System.out.println("Waiting for " + jobFutures.size() + " jobs to complete...");
    CompletableFuture<Void> allJobs = CompletableFuture.allOf(jobFutures.toArray(new CompletableFuture[0]));

    try {
        allJobs.join();

        long successfulJobs = jobFutures.stream()
            .mapToLong(future -> {
                try {
                    return future.get() ? 1L : 0L;
                } catch (Exception e) {
                    return 0L;
                }
            })
            .sum();

        System.out.println("All jobs completed. Successful: " + successfulJobs + "/" + jobFutures.size());

    } catch (Exception e) {
        System.err.println("Error waiting for jobs to complete: " + e.getMessage());
    }

    long endTime = System.currentTimeMillis();
    long duration = endTime - startTime;
    TestReport(duration);
}

    


         public void TestReport(long duration) {
        
            String[] logMessage = {
            ItemDistributorService.getHourWithAmPm(),
            "Test Environment",
            "Test completed in " + duration + " ms"
        };
        logManager.addLog(logMessage);

        System.out.println("Average normal job time: " + jobs.getAverageNormalJobTime());
        System.out.println("Average priority job time: " + jobs.getAveragePriorityJobTime());
        System.out.println("Total completed jobs: " + jobs.completedJobs());
        System.out.println("Total battery usage: " + jobs.getTotalBatteryUsage());
    System.out.println("Total double jobs: " + dualJobCount.get());
    String[] mergeSummary = { ItemDistributorService.getHourWithAmPm(), "MERGE", "Total double jobs: " + dualJobCount.get() };
    logManager.addLog(mergeSummary);
        // System.out.println("Average battery usage per job: " + (jobs.getTotalBatteryUsage() / (double) jobs.completedJobs().size()));


    }


private static class DeterministicItemScheduler {
    private final List<ScheduledItem> scheduledItems = new ArrayList<>();
    
    public void scheduleItems() {
        scheduledItems.add(new ScheduledItem(0, 10, false));  
        
        scheduledItems.add(new ScheduledItem(4000, 8, false));    
        scheduledItems.add(new ScheduledItem(8000, 1, true));     
        scheduledItems.add(new ScheduledItem(9000, 8, false));   
        scheduledItems.add(new ScheduledItem(12000, 1, true));   
        scheduledItems.add(new ScheduledItem(13000, 8, false)); 
        scheduledItems.add(new ScheduledItem(16000, 1, true));    
        scheduledItems.add(new ScheduledItem(17000, 8, false));   
        scheduledItems.add(new ScheduledItem(20000, 1, true));    
        scheduledItems.add(new ScheduledItem(21000, 8, false));  
        scheduledItems.add(new ScheduledItem(25000, 1, true));    
        scheduledItems.add(new ScheduledItem(26000, 8, false));   
        scheduledItems.add(new ScheduledItem(30000, 8, false));   
    }
    
    public List<ScheduledItem> getItemsAtTime(long currentTime) {
        return scheduledItems.stream()
            .filter(item -> item.scheduledTime <= currentTime && !item.added)
            .peek(item -> item.added = true)
            .collect(Collectors.toList());
    }
    
    
}


    
}
