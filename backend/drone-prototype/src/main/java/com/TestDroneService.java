// DroneService Class - this class manages drone operations, including job creation, pathfinding, and movement. It interacts with various components like jobs, map, battery stations, and pick-up stations to facilitate drone tasks.

package com;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static java.util.concurrent.Executors.newFixedThreadPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class TestDroneService {
    private final ConcurrentHashMap<String, Drone> drones;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    public final ReentrantLock lock = new ReentrantLock();

    private final ScheduledExecutorService scheduler =
        Executors.newScheduledThreadPool(10); 
    private final service jobs;
    private final Map map;
    private final List<Location> targets;   
    private final List<BatteryStation> batteryStations;
    private final List<PickUpStation> pickUpStations;
    private final ExecutorService pathfindingExecutor = newFixedThreadPool(14);
    private final ExecutorService moveExecutor = Executors.newFixedThreadPool(14);
    private final Random random = new Random();
    private final LogManager logManager;
    private final Pathfinder pathfinder = new Pathfinder();


    @Autowired
    public TestDroneService(ConcurrentHashMap<String, Drone> TestDrones, service jobs, Map map,
                        List<Location> TestTargets, List<BatteryStation> TestBatteryStations,
                        List<PickUpStation> TestPickUpStations, LogManager logManager) {
        this.drones = TestDrones;
        this.jobs = jobs;
        this.map = map;
        this.targets = TestTargets;
        this.batteryStations = TestBatteryStations;
        this.logManager = logManager;
        this.pickUpStations = TestPickUpStations;
    }

    public List<Drone> getAllDrones() {
        return new ArrayList<>(drones.values());
    }

    // Get all paths for drones that are currently busy
    // Structure = map of drone IDs to their paths
    public java.util.Map<String, List<node>> getCurrentPaths() {
        java.util.Map<String, List<node>> paths = new HashMap<>();
    
        for (java.util.Map.Entry<String, Drone> entry : drones.entrySet()) {
            Drone drone = entry.getValue();
            if (drone.getAvailable().equals(DroneStatus.BUSY.getCode())) {
                String jobId = drone.getDroneid() + "-JOB";
                Job job = jobs.getJobById(jobId);
                if (job != null && job.getFullPath() != null) {
                    paths.put(drone.getDroneid(), new ArrayList<>(job.getFullPath()));
                }
            }
        }
        return paths;
    }
    

    public void moveAllJobs() {
        jobs.getAllJobs();
    }

    //Take in drone and item, create a job for the drone to pick up the item from the pick-up station and deliver it to a random target.
    //if the drone is not available, it will not create a job for it. (send to battery station or ignore)
    public synchronized CompletableFuture<Boolean> createJob(Drone drone, ItemForDelivery item, Location destinationn) {
    CompletableFuture<Boolean> jobFuture = new CompletableFuture<>();
    
    if (!drone.getAvailable().equals(DroneStatus.AVAILABLE.getCode())) {
        jobFuture.complete(false);
        System.out.println("FAILURE: Drone " + drone.getDroneid() + " is not available for item " + item.getItemId());

        return jobFuture;
    }

    ItemForDelivery.CurrentItemsAvailable.removeItem(item);
    Location destination = destinationn;
    final PickUpStation pickUpStation = item.getPickUpStation();
    final node pickUpLocation = pickUpStation.getLocation();

   
    if (destination != null) {
        node start = new node((int) drone.getX(), (int) drone.getY());
        node end = map.getNode(destination.getNode().x, destination.getNode().y);

        // CHECK IF OUR BATTERY WILL RUN OUT BEFORE REACHING DESTINATION IF SO SAY NO
        if (drone.calculateBatteryUsage(start.distanceTo(pickUpLocation) + pickUpLocation.distanceTo(end)) > drone.getBattery()) {
            
            drone.setAvailable(DroneStatus.RECHARGING.getCode());
            System.out.println("FAILURE: Drone " + drone.getDroneid() + " does not have enough battery for item " + item.getItemId());

            String[] logMessage = {
                getHourWithAmPm() + " ",
                "Job Failed To Assign",
                "Failed job - No Battery: " + drone.getDroneid()
            };
            logManager.addLog(logMessage);
            sendToNearestBatteryStation(drone);
          
            ItemForDelivery.CurrentItemsAvailable.addItem(item);
            jobs.removeJob(drone.getDroneid() + "-JOB");

            jobFuture.complete(false);
            return jobFuture;
        }

        String jobid = drone.getDroneid() + "-JOB";
        drone.setAvailable(DroneStatus.BUSY.getCode());
        String[] logMessage = {
            getHourWithAmPm() + " ",
            "Job Assigned",
            "Created job - Assigned Drone: " + drone.getDroneid() + " to Item: " + item.getItemName()
        };

        logManager.addLog(logMessage);   

        pathfindingExecutor.submit(() -> {

            try {
                List<node> pathToPickUp = pathfinder.findPath(map, map.getNode(start.x, start.y), map.getNode(pickUpLocation.x, pickUpLocation.y));
                List<node> pathFromPickUpToDestination = pathfinder.findPath(map, map.getNode(pickUpLocation.x, pickUpLocation.y), end);

                if (pathToPickUp == null || pathToPickUp.isEmpty()) {
                    System.err.println("No path found for drone: " + drone.getDroneid());
                    drone.setAvailable(DroneStatus.AVAILABLE.getCode());
                    jobFuture.complete(false);
                    return;
                }

                Job job = new Job(jobid, item, drone, destination, pathToPickUp, pathFromPickUpToDestination);
                jobs.addJob(job);
                ItemForDelivery.CurrentItemsAvailable.removeItem(item);

                try {
                    eventPublisher.publishEvent(new PathsUpdatedEvent(getCurrentPaths()));
                } catch (Exception e) {
                    System.err.println("WebSocket broadcast failed: " + e.getMessage());
                    
                }
            
                double usage = drone.calculateBatteryUsage(start.distanceTo(pickUpLocation) + pickUpLocation.distanceTo(end));
    
                moveExecutor.submit(() -> {
                    moveToTarget(drone, usage, pickUpStation, item)
                        .thenAccept(result -> {
                            if (result) {
                                drone.setAvailable(DroneStatus.AVAILABLE.getCode());
                
                                String[] logMessage2 = {
                                    getHourWithAmPm(),
                                    "Job Completed ",
                                    "Item Delivered: " + item.getItemId() + " by Drone: " + drone.getDroneid()
                                };
                
                                logManager.addLog(logMessage2);
                                jobs.removeJob(jobid);
                                jobs.addToTotalBatteryUsage((int) usage);
                                job.setTimeStarted(item.addedTo);
                                job.setTimeCompleted(System.currentTimeMillis());
                                job.setDuration();
                
                                jobs.addToJobStack(job);
                
                                if (item.priority) {
                                    jobs.allPriorityJobTimes.add(job.getJobDuration());
                                    System.out.println("Priority job for " + item.getItemId() + " took " + job.getJobDuration() + " milliseconds");
                                } else {
                                    jobs.allNormalJobTimes.add(job.getJobDuration());
                                    System.out.println("Normal job for " + item.getItemId() + " took " + job.getJobDuration() + " milliseconds");
                                }
                
                                jobFuture.complete(true);
                
                            } else {
                                String[] logMessage3 = {
                                    getHourWithAmPm(),
                                    "Job Failed ",
                                    "Failed job: " + jobid
                                };
                
                                logManager.addLog(logMessage3);
                
                                drone.setAvailable(DroneStatus.AVAILABLE.getCode());
                                jobs.removeJob(jobid);
                                System.out.println("FAILURE: Drone " + drone.getDroneid() + " failed to deliver item " + item.getItemId());
                
                                jobFuture.complete(false);
                            }
                        })
                        .exceptionally(ex -> {
                            System.err.println("Exception during moveToTarget for drone " + drone.getDroneid() + ": " + ex.getMessage());
                            drone.setAvailable(DroneStatus.AVAILABLE.getCode());
                            jobs.removeJob(jobid);
                            jobFuture.complete(false);
                            return null;
                        });
                });

            } catch (Exception e) {
                System.err.println("Pathfinding error for drone " + drone.getDroneid() + ": " + e.getMessage());
                drone.setAvailable(DroneStatus.AVAILABLE.getCode());
                jobs.removeJob(jobid);
                jobFuture.completeExceptionally(e);
            }
        });
        
    } else {
        System.err.println("No destination set for drone: " + drone.getDroneid());
        jobFuture.complete(false);
    }

    return jobFuture;
}


    public synchronized CompletableFuture<Boolean> createJobDouble(Drone drone, ItemForDelivery item, Location destination,  ItemForDelivery item2, Location destination2) {
        CompletableFuture<Boolean> jobFuture = new CompletableFuture<>();
        
        if (!drone.getAvailable().equals(DroneStatus.AVAILABLE.getCode())) {
            jobFuture.complete(false);
            System.out.println("FAILURE: Drone " + drone.getDroneid() + " is not available for item " + item.getItemId());

            return jobFuture;
        }

    ItemForDelivery.CurrentItemsAvailable.removeItem(item);
    ItemForDelivery.CurrentItemsAvailable.removeItem(item2);

        Location destinationOne = destination;
        Location destinationTwo = destination2;


        final PickUpStation pickUpStation = item.getPickUpStation();
        final node pickUpLocation = pickUpStation.getLocation();

        if (pickUpLocation != null && destinationOne != null && destinationTwo != null) {

            node start = new node((int) drone.getX(), (int) drone.getY());
            node firstTarget = map.getNode(destinationOne.getNode().x, destinationOne.getNode().y);
            node secondTarget = map.getNode(destinationTwo.getNode().x, destinationTwo.getNode().y);

            double distFirst = pickUpLocation.distanceTo(firstTarget);
            double distSecond = pickUpLocation.distanceTo(secondTarget);
            if (distSecond < distFirst) {
                Location tmpLoc = destinationOne; destinationOne = destinationTwo; destinationTwo = tmpLoc;
                node tmpNode = firstTarget; firstTarget = secondTarget; secondTarget = tmpNode;
                ItemForDelivery tmpItem = item; item = item2; item2 = tmpItem;
            }
            final Location fDestinationOne = destinationOne;
            final Location fDestinationTwo = destinationTwo;
            final ItemForDelivery fItem1 = item;
            final ItemForDelivery fItem2 = item2;
            final node fFirstTarget = firstTarget;
            final node fSecondTarget = secondTarget;

            // distance from current location, to first target, then to second target.
            if (drone.calculateBatteryUsage(start.distanceTo(pickUpLocation) + pickUpLocation.distanceTo(firstTarget) + firstTarget.distanceTo(secondTarget)) > drone.getBattery()) {

                drone.setAvailable(DroneStatus.RECHARGING.getCode());
                System.out.println("FAILURE: Drone " + drone.getDroneid() + " does not have enough battery for item " + item.getItemId());

                String[] logMessage = {
                    getHourWithAmPm() + " ",
                    "Job Failed To Assign",
                    "Failed job - No Battery: " + drone.getDroneid()
                };
                logManager.addLog(logMessage);
                sendToNearestBatteryStation(drone);
            
                ItemForDelivery.CurrentItemsAvailable.addItem(item);
                ItemForDelivery.CurrentItemsAvailable.addItem(item2);
                jobs.removeJob(drone.getDroneid() + "-JOB");

                jobFuture.complete(false);
                return jobFuture;
            }

            String jobid = drone.getDroneid() + "-JOB";
            drone.setAvailable(DroneStatus.BUSY.getCode());
            String[] logMessage = {
                getHourWithAmPm() + " ",
                "Dual Job Assigned",
                "Assigned Drone: " + drone.getDroneid() + " Items: [First: " + item.getItemName() + ", Second: " + item2.getItemName() + "]"
            };

            logManager.addLog(logMessage);   

            pathfindingExecutor.submit(() -> {

                try {
                    List<node> pathToPickUp = pathfinder.findPath(map, map.getNode(start.x, start.y), map.getNode(pickUpLocation.x, pickUpLocation.y));
                    List<node> pathFromPickUpToFirstTarget = pathfinder.findPath(map, map.getNode(pickUpLocation.x, pickUpLocation.y), fFirstTarget);
                    List<node> pathFromFirstTargetToSecondTarget = pathfinder.findPath(map, map.getNode(fFirstTarget.x, fFirstTarget.y), fSecondTarget);



                    if (pathToPickUp == null || pathToPickUp.isEmpty()) {
                        System.err.println("No path found for drone: " + drone.getDroneid());
                        drone.setAvailable(DroneStatus.AVAILABLE.getCode());
                        jobFuture.complete(false);
                        return;
                    }


                    DoubleJob job = new DoubleJob(jobid, fItem1, drone, fDestinationOne, fItem2, fDestinationTwo, pathToPickUp, pathFromPickUpToFirstTarget, pathFromFirstTargetToSecondTarget);
                    jobs.addDoubleJob(job);


                    try {
                        eventPublisher.publishEvent(new PathsUpdatedEvent(getCurrentPaths()));
                    } catch (Exception e) {
                        System.err.println("WebSocket broadcast failed: " + e.getMessage());
                        
                    }
                
                    double usage = drone.calculateBatteryUsage(start.distanceTo(pickUpLocation) + pickUpLocation.distanceTo(fFirstTarget) + fFirstTarget.distanceTo(fSecondTarget));
        
                    moveExecutor.submit(() -> {
                        moveToTargetDoubleJob(drone, usage, pickUpStation, fItem1, fItem2)
                            .thenAccept(result -> {
                                if (result) {
                                    drone.setAvailable(DroneStatus.AVAILABLE.getCode());
                    
                                    String[] logMessage2 = {
                                        getHourWithAmPm(),
                                        "Dual Job Completed ",
                                        "Items Delivered: " + fItem1.getItemId() + ", " + fItem2.getItemId() + " by Drone: " + drone.getDroneid()
                                    };
                    
                                    logManager.addLog(logMessage2);
                                    jobs.removeJob(jobid);
                                    jobs.addToTotalBatteryUsage((int) usage);
                                    job.setTimeStarted(fItem1.addedTo);
                                    job.setTimeCompleted(System.currentTimeMillis());
                                    job.setDuration();
                    
                                    jobs.addToJobStack(job);
                    
                                    if (fItem1.priority) {
                                        jobs.allPriorityJobTimes.add(job.getJobDuration());
                                        System.out.println("Priority job for " + fItem1.getItemId() + " took " + job.getJobDuration() + " milliseconds");
                                    } else {
                                        jobs.allNormalJobTimes.add(job.getJobDuration());
                                        System.out.println("Normal job for " + fItem1.getItemId() + " took " + job.getJobDuration() + " milliseconds");
                                    }
                    
                                    jobFuture.complete(true);
                    
                                   } else {
                                    String[] logMessage3 = {
                                        getHourWithAmPm(),
                                        "Dual Job Failed ",
                                        "Failed dual job: " + jobid
                                    };
                    
                                    logManager.addLog(logMessage3);
                    
                                    drone.setAvailable(DroneStatus.AVAILABLE.getCode());
                                    jobs.removeJob(jobid);
                                    System.out.println("FAILURE: Drone " + drone.getDroneid() + " failed to deliver both items " + fItem1.getItemId() + ", " + fItem2.getItemId());
                    
                                    jobFuture.complete(false);
                                }
                            })
                            .exceptionally(ex -> {
                                System.err.println("Exception during moveToTarget for drone " + drone.getDroneid() + ": " + ex.getMessage());
                                drone.setAvailable(DroneStatus.AVAILABLE.getCode());
                                jobs.removeJob(jobid);
                                jobFuture.complete(false);
                                return null;
                            });
                    });

                } catch (Exception e) {
                            System.err.println("Dual pathfinding error for drone " + drone.getDroneid() + ": " + e.getMessage());
                    drone.setAvailable(DroneStatus.AVAILABLE.getCode());
                    jobs.removeJob(jobid);
                    jobFuture.completeExceptionally(e);
                }
            });
            
        } else {
            System.err.println("No destination(s) set for drone: " + drone.getDroneid());
            jobFuture.complete(false);
        }

        return jobFuture;
    }


    public CompletableFuture<Boolean> moveToTarget(Drone drone, double usage, PickUpStation pickUpStation, ItemForDelivery item) {
        String jid = drone.getDroneid() + "-JOB";
        Job thisJob = jobs.getJobById(jid);
        if (thisJob == null) {
            System.err.println("Job not found for drone: " + drone.getDroneid());
            return CompletableFuture.completedFuture(false);
        }

        List<node> pathToPickUp = thisJob.getPathToPickUp();
        List<node> pathToDestination = thisJob.getPathToDestination();

        if (pickUpStation == null) {
            System.err.println("No pick-up station available for drone: " + drone.getDroneid());
            drone.setAvailable(DroneStatus.AVAILABLE.getCode());
            return CompletableFuture.completedFuture(false);
        }

        return moveToPickUpStation(drone, pickUpStation, pathToPickUp)
            .thenCompose(pickUpSuccess -> {
                if (!pickUpSuccess) {
                    System.err.println("Drone " + drone.getDroneid() + " failed to reach pick-up station.");
                    return CompletableFuture.completedFuture(false);
                }
                return moveToDestination(drone, pathToDestination);
            })
            .thenApply(destSuccess -> {
                if (!destSuccess) {
                    System.err.println("Drone " + drone.getDroneid() + " failed to reach destination.");
                }
                return destSuccess;
            });


    }


    public CompletableFuture<Boolean> moveToTargetDoubleJob(Drone drone, double usage, PickUpStation pickUpStation, ItemForDelivery item, ItemForDelivery secondItem) {
        String jid = drone.getDroneid() + "-JOB";
        DoubleJob thisJob = jobs.getDoubleJobById(jid);
        if (thisJob == null) {
            System.err.println("Job not found for drone: " + drone.getDroneid());
            return CompletableFuture.completedFuture(false);
        }

        List<node> pathToPickUp = thisJob.getPathToPickUp();
        List<node> pathToDestination = thisJob.getPathToDestination();
        List<node> pathFromFirstTargetToSecondTarget = thisJob.getPathFromFirstTargetToSecondTarget();

        System.out.println("[DUAL] Paths => pickup:" + (pathToPickUp==null?0:pathToPickUp.size()) + 
            " firstLeg:" + (pathToDestination==null?0:pathToDestination.size()) + 
            " secondLeg:" + (pathFromFirstTargetToSecondTarget==null?0:pathFromFirstTargetToSecondTarget.size()));

        if (pickUpStation == null) {
            System.err.println("No pick-up station available for drone: " + drone.getDroneid());
            drone.setAvailable(DroneStatus.AVAILABLE.getCode());
            return CompletableFuture.completedFuture(false);
        }

        return moveToPickUpStation(drone, pickUpStation, pathToPickUp)
            .thenCompose(pickUpSuccess -> {
                if (!pickUpSuccess) {
                    System.err.println("[DUAL] Drone " + drone.getDroneid() + " failed to reach pick-up station.");
                    return CompletableFuture.completedFuture(false);
                }
                System.out.println("[DUAL] Drone " + drone.getDroneid() + " collected both items at station.");
                return moveToDestination(drone, pathToDestination);
            })
            .thenCompose(firstLegSuccess -> {
                if (!firstLegSuccess) {
                    System.err.println("[DUAL] Drone " + drone.getDroneid() + " failed en route to first drop.");
                    return CompletableFuture.completedFuture(false);
                }
                System.out.println("[DUAL] Drone " + drone.getDroneid() + " delivered FIRST item " + item.getItemId());
                if (pathFromFirstTargetToSecondTarget == null || pathFromFirstTargetToSecondTarget.isEmpty()) {
                    System.out.println("[DUAL] No second leg path (same destination) treated as success.");
                    return CompletableFuture.completedFuture(true);
                }
                node secStart = pathFromFirstTargetToSecondTarget.get(0);
                node secEnd = pathFromFirstTargetToSecondTarget.get(pathFromFirstTargetToSecondTarget.size()-1);
                System.out.println("[DUAL] Second leg start=" + secStart.x + "," + secStart.y + " end=" + secEnd.x + "," + secEnd.y + " length=" + pathFromFirstTargetToSecondTarget.size());
                CompletableFuture<Void> pause = new CompletableFuture<>();
                scheduler.schedule(() -> {
                    System.out.println("[DUAL] Pause after first drop complete. Beginning second leg...");
                    pause.complete(null);
                }, 1, TimeUnit.SECONDS);
                return pause.thenCompose(v -> moveToDestination(drone, pathFromFirstTargetToSecondTarget))
                        .thenApply(secondLegSuccess -> {
                            if (!secondLegSuccess) {
                                System.err.println("[DUAL] Drone " + drone.getDroneid() + " failed en route to second drop.");
                                return false;
                            }
                            System.out.println("[DUAL] Drone " + drone.getDroneid() + " delivered SECOND item " + secondItem.getItemId());
                            return true;
                        });
            });
    }





    public CompletableFuture<Boolean> moveToPickUpStation(Drone drone, PickUpStation pickUpStation, List<node> pathToPickUp) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
    
        if (pathToPickUp == null || pathToPickUp.isEmpty()) {
            System.err.println("No path found for drone: " + drone.getDroneid());
            drone.setAvailable(DroneStatus.AVAILABLE.getCode());
            future.complete(false);
            return future;
        }
    
        final double batteryPerNode = 0.1;
        final Iterator<node> iterator = pathToPickUp.iterator();
    
        ScheduledFuture<?> scheduled = scheduler.scheduleAtFixedRate(() -> {
            if (!iterator.hasNext()) {
                pickUpStation.requestPickup(drone);
                future.complete(true);
                throw new RuntimeException("STOP"); 
            }
    
            node next = iterator.next();
            drone.moveTo(next.x, next.y);
            drone.setBattery(drone.getBattery() - batteryPerNode);
    
            if (drone.getBattery() <= 0) {
                drone.setAvailable(DroneStatus.AVAILABLE.getCode());
                future.complete(false);
                throw new RuntimeException("STOP");
            }
    
        }, 0, 75, TimeUnit.MILLISECONDS);
    
        future.whenComplete((result, error) -> scheduled.cancel(false));
    
        return future;
    }
    



    public CompletableFuture<Boolean> moveToDestination(Drone drone, List<node> pathToDestination) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        if (pathToDestination == null || pathToDestination.isEmpty()) {
            future.complete(false);
            return future;
        }
    
        final double batteryPerNode = 0.1;
        final Iterator<node> iterator = pathToDestination.iterator();
    
        ScheduledFuture<?> scheduled = scheduler.scheduleAtFixedRate(() -> {
            if (!iterator.hasNext()) {
                future.complete(true);
                throw new RuntimeException("STOP");
            }
    
            node next = iterator.next();
            drone.moveTo(next.x, next.y);
            drone.setBattery(drone.getBattery() - batteryPerNode);
    
            if (drone.getBattery() <= 0) {
                future.complete(false);
                throw new RuntimeException("STOP");
            }
    
        }, 0, 75, TimeUnit.MILLISECONDS);
    
        future.whenComplete((res, ex) -> scheduled.cancel(false));
    
        return future;
    }
    
    



    public void shutdown() {
        pathfindingExecutor.shutdown();
        try {
            if (!pathfindingExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                pathfindingExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            pathfindingExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    public static String getHourWithAmPm() {
        SimpleDateFormat formatter = new SimpleDateFormat("h:mm a");
        return formatter.format(new Date(System.currentTimeMillis()));
    }



    //find nearest 'availiable' battery station to the drone and send it there
   public void sendToNearestBatteryStation(Drone drone) {
    CompletableFuture.runAsync(() -> {
        boolean sent = false;
        while (!sent) {
            List<BatteryStation> sortedStations = new ArrayList<>(batteryStations);
            sortedStations.sort((a, b) -> {
                double distA = Math.pow(drone.getX() - a.getLocation().x, 2) +
                            Math.pow(drone.getY() - a.getLocation().y, 2);
                double distB = Math.pow(drone.getX() - b.getLocation().x, 2) +
                             Math.pow(drone.getY() - b.getLocation().y, 2);
                return Double.compare(distA, distB);
            });

            BatteryStation nearestStation = null;
            for (BatteryStation station : sortedStations) {
                if (1 == 1) {
                    nearestStation = station;
                    break;
                }
            }

            if (nearestStation != null) {
                final BatteryStation finalNearestStation = nearestStation;
                System.out.println("Drone " + drone.getDroneid() + " is moving to battery station: " + finalNearestStation.getName());
                boolean completed = moveToBS(drone, finalNearestStation.getX(), finalNearestStation.getY());
                if (completed) {
                    finalNearestStation.addDrone(drone);
                    sent = true;
                } else {
                    System.err.println("Drone " + drone.getDroneid() + " failed to reach battery station.");
                    
                }
            } else {
                System.err.println("No available battery stations for drone " + drone.getDroneid() + ", retrying...");
                try {
                    Thread.sleep(500); 
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    });
}


    //function to move drone to battery station, after replacing the battery.
    //simple pathfinding as before, here we simply move to target but do not broadcast thr path;
    public boolean moveToBS(Drone drone, double x, double y) {

        List<node> path = pathfinder.findPath(map, map.getNode((int) drone.getX(), (int) drone.getY()), map.getNode((int) x, (int) y));

        for (node next : path) {
            drone.moveTo(next.x, next.y);
            try {
                Thread.sleep(50); 
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            }
        }
        String[] logMessage = {
            getHourWithAmPm(),
            "Reacharging Drone",
            "Drone " + drone.getDroneid() + " moved to battery station at (" + x + " - " + y + ")"
        };
        logManager.addLog(logMessage);
        return true;
    }

    public Location randomTarget() {
        return targets.get(random.nextInt(targets.size()));
    }


    // public void allJobs() {
    //     System.out.println("creating jobs for all drones");
    //     for (ItemForDelivery item : ItemForDelivery.CurrentItemsAvailable.getItemsForDelivery()) {
    //         Drone drone = findClosestAvailableDrone(item.getPickUpStation().getLocation());
    //         if (drone == null) {
    //             System.out.println("No available drones found.");
    //             return;
    //         }
    //        createJob(drone, item);
    //     }
    // }

    public Drone findClosestAvailableDrone(node location) {
        double minDistance = Double.MAX_VALUE;
        Drone closestDrone = null;
    
        List<Drone> availableDrones = drones.values().stream()
            .filter(drone -> drone.getAvailable().equals(DroneStatus.AVAILABLE.getCode()))
            .sorted(Comparator.comparing(Drone::getDroneid))
            .collect(Collectors.toList());
    
        for (Drone drone : availableDrones) {
            double distance = Math.pow(drone.getX() - location.x, 2) +
                              Math.pow(drone.getY() - location.y, 2);
            if (distance < minDistance) {
                minDistance = distance;
                closestDrone = drone;
            }
        }
    
        return closestDrone;
    }
    


}

