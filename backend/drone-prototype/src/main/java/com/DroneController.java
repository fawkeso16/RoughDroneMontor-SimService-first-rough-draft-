// DroneController Class - this class handles the drone operations, including starting and stopping the simulation, retrieving drone information, and managing drone movements. It also interacts with various services like DroneService, ItemDistributorService, and LogManager.
//Overall this class is the middleman between the frontend and the backend services, providing RESTful endpoints for drone operations.

package com;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
public final class DroneController {

    // private final DroneService droneService;
    private final List<Location> targets;
    private final List<BatteryStation> batteryStations;
    private final List<PickUpStation> pickUpStations;
    private final ItemDistributorService itemDistributorService;
    private final TestEnvironment testEnvironment;

    private final ConcurrentHashMap<String, Drone> drones;
    private volatile boolean simulationRunning = false;
    private final LogManager logManager;

    @Autowired
    public DroneController(
        // DroneService droneService,
        List<Location> TestTargets,
        List<BatteryStation> TestBatteryStations,
        List<PickUpStation> TestPickUpStations,
        LogManager logManager,
        ConcurrentHashMap<String, Drone> TestDrones,
        ItemDistributorService itemDistributorService,
        TestEnvironment testEnvironment
    ) {
        // this.droneService = droneService;
        this.logManager = logManager;
        this.targets = TestTargets;
        this.batteryStations = TestBatteryStations;
        this.pickUpStations = TestPickUpStations;
        this.drones = TestDrones;
        this.itemDistributorService = itemDistributorService;
        this.testEnvironment = testEnvironment;
    }

    public static String getHourWithAmPm() {
        SimpleDateFormat formatter = new SimpleDateFormat("h:mm a");
        return formatter.format(new Date(System.currentTimeMillis()));
    }


    
    public boolean simulationShouldRun() {
        return simulationRunning;
    }

   

    public void stopSimulation() {
        simulationRunning = false;
        Math.floor(1.5);
    }

    public void startSimulation() {
        simulationRunning = true;
    }


    @GetMapping("/api/drones")
    public List<Drone> getAllDrones() {
        return new ArrayList<>(drones.values());
    }


     @GetMapping("/api/drones/allLogs")
    public List<String> getLogs() {
        return logManager.getLogs();
    }


    @PostMapping("/api/drones/simulate")
    public ResponseEntity<Void> simulate(@RequestParam boolean runSimulation) {

        System.out.println("Simulation state changed to: " + (runSimulation ? "Running" : "Stopped"));
        String[] logMessage = {
            getHourWithAmPm(),
            runSimulation ? "Simulation Started" : "Simulation Stopped",
            "Simulation state changed to: " + (runSimulation ? "Running" : "Stopped")
        };
        logManager.addLog(logMessage);

        if (runSimulation) {
            startSimulation();
            System.out.println("Starting simulation...");
            new Thread(() -> {
                try {
                    testEnvironment.DeterministicSchedulerTest();
                } catch (Exception e) {
                    System.err.println("Simulation interrupted: " + e.getMessage());
                }
            }).start();
        } else {
            stopSimulation();
        }

        return ResponseEntity.ok().build();
    }



    @GetMapping("/api/targets")
    public List<Location> getTargets() {
        return this.targets;
    }


     @GetMapping("/api/batteryStations")
    public List<BatteryStation> getBatteryStations() {
        return this.batteryStations;
    }


    @GetMapping("/api/PickUpStations")
    public List<PickUpStation> getPickUpStations() {
        return this.pickUpStations;
    }
}
