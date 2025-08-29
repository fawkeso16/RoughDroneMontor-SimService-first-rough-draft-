package com;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    
    @Bean
    public Map map() {
        return new Map(190); 
    }

    @Bean
    public LogManager logManager() {
        return new LogManager(100);
    }

    @Bean
    public ConcurrentHashMap<String, Drone> drones() {
        ConcurrentHashMap<String, Drone> drones = new ConcurrentHashMap<>();
        drones.put("DRONE001", new Drone("DRONE001", randomCoord(), randomCoord(), 100));
        drones.put("DRONE002", new Drone("DRONE002", randomCoord(), randomCoord(), 100));
        drones.put("DRONE003", new Drone("DRONE003", randomCoord(), randomCoord(), 100));
        drones.put("DRONE004", new Drone("DRONE004", randomCoord(), randomCoord(), 100));
        drones.put("DRONE005", new Drone("DRONE005", randomCoord(), randomCoord(), 100));
        drones.put("DRONE006", new Drone("DRONE006", randomCoord(), randomCoord(), 100));
        drones.put("DRONE007", new Drone("DRONE007", randomCoord(), randomCoord(), 100));
        drones.put("DRONE008", new Drone("DRONE008", randomCoord(), randomCoord(), 100));
        drones.put("DRONE009", new Drone("DRONE009", randomCoord(), randomCoord(), 100));
        drones.put("DRONE010", new Drone("DRONE010", randomCoord(), randomCoord(), 100));
        drones.put("DRONE011", new Drone("DRONE011", randomCoord(), randomCoord(), 100));
        drones.put("DRONE012", new Drone("DRONE012", randomCoord(), randomCoord(), 100));
        drones.put("DRONE013", new Drone("DRONE013", randomCoord(), randomCoord(), 100));
        drones.put("DRONE014", new Drone("DRONE014", randomCoord(), randomCoord(), 100));
        return drones;
    }


    @Bean
    public ConcurrentHashMap<String, Drone> TestDrones() {
        ConcurrentHashMap<String, Drone> testDrones = new ConcurrentHashMap<>();
        testDrones.put("DRONE001", new Drone("DRONE001", 123, 100, 100));
        testDrones.put("DRONE002", new Drone("DRONE002", 25, 32, 100));
        testDrones.put("DRONE003", new Drone("DRONE003", 111, 17, 100));
        testDrones.put("DRONE004", new Drone("DRONE004", 45, 67, 100));
        testDrones.put("DRONE005", new Drone("DRONE005", 23, 89, 100));
        testDrones.put("DRONE006", new Drone("DRONE006", 78, 56, 100));
        testDrones.put("DRONE007", new Drone("DRONE007", 90, 10, 100));
        testDrones.put("DRONE008", new Drone("DRONE008", 66, 32, 100));
        testDrones.put("DRONE009", new Drone("DRONE009", 99, 187, 100));
        testDrones.put("DRONE010", new Drone("DRONE010", 187, 99, 100));
        return testDrones;
    }
    


    private static int randomCoord() {
        return (int) (Math.random() * 190);
    }

    @Bean
    public List<Location> targets(Map map) {
        List<Location> targets = new ArrayList<>();
        for (int i = 0; i < 80; i++) {
            node m = map.getRandomNode();
            node n = new node(m.x, m.y);
            targets.add(new Location(n));
        }
        return targets;
    }

    @Bean
    public List<Location> TestTargets(Map map) {
        List<Location> targets = new ArrayList<>();

        int[][] coords = {
            {5, 10}, {12, 34}, {45, 67}, {23, 89}, {78, 56},
            {90, 10}, {66, 32}, {14, 99}, {100, 3}, {7, 7},
            {55, 55}, {33, 77}, {60, 120}, {180, 10}, {170, 170},
            {160, 45}, {150, 150}, {145, 30}, {134, 67}, {122, 88},
            {119, 43}, {111, 60}, {108, 108}, {99, 99}, {88, 88},
            {77, 33}, {65, 65}, {50, 50}, {40, 40}, {30, 30},
            {20, 20}, {10, 10}, {0, 0}, {5, 145}, {187, 0},
            {100, 187}, {187, 187}, {80, 140}, {141, 80}, {10, 180},
            {33, 44}, {44, 33}, {25, 75}, {75, 25}, {123, 123},
            {150, 5}, {5, 150}, {60, 170}, {170, 60}, {99, 187}
        };

        for (int[] coord : coords) {
            node n = new node(coord[0], coord[1]);
            targets.add(new Location(n));
        }

        return targets;
    }

    @Bean
    public List<BatteryStation> batteryStations(Map map) {
        List<BatteryStation> batteryStations = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            node m = map.getRandomNode();
            node n = new node(m.x, m.y);
            batteryStations.add(new BatteryStation("BatteryStation-" + i, n));
            System.out.println("Created Battery Station: " + n);
        }
        return batteryStations;
    }

    @Bean
    public List<BatteryStation> TestBatteryStations(Map map) {
        List<BatteryStation> batteryStations = new ArrayList<>();

        int[][] coords = {
            {10, 10}, {30, 40}, {60, 90}, {90, 60},
            {120, 130}, {160, 20}, {170, 175}
        };

        for (int i = 0; i < coords.length; i++) {
            node n = new node(coords[i][0], coords[i][1]);
            batteryStations.add(new BatteryStation("BatteryStation-" + i, n));
            System.out.println("Created Battery Station: " + n);
        }

        return batteryStations;
    }

    @Bean
    public List<PickUpStation> pickUpStations(Map map) {
        List<PickUpStation> pickUpStations = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            node m = map.getRandomNode();
            node n = new node(m.x, m.y);
            pickUpStations.add(new PickUpStation("PickUpStation-" + i, n));
            // System.out.println("Created Pick Up Station: " + n);
        }
        return pickUpStations;
    }

    @Bean
    public List<PickUpStation> TestPickUpStations(Map map) {
        List<PickUpStation> pickUpStations = new ArrayList<>();

        int[][] coords = {
            {25, 40}, {66, 130}, {145, 145}, {140, 60}
        };

        for (int i = 0; i < coords.length; i++) {
            node n = new node(coords[i][0], coords[i][1]);
            pickUpStations.add(new PickUpStation("PickUpStation-" + i, n));
            // System.out.println("Created Pick Up Station: " + n);
        }

        return pickUpStations;
    }

}