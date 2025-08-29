// DroneDistributorService Class - this class is responsible for managing the distribution of items to drones, including initializing items, scheduling item additions, and providing access to the current items available for delivery.

package com;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class ItemDistributorService {

    private final List<PickUpStation> pickUpStations;
    private final DeliveryEstimationService deliveryEstimationService;
    private final LogManager logManager;
    private ConcurrentHashMap<String, Drone> drones;
    private final Random random = new Random();
    private final ExecutorService itemAdderExecutor = Executors.newSingleThreadScheduledExecutor();


    public static String getHourWithAmPm() {
        SimpleDateFormat formatter = new SimpleDateFormat("h:mm a");
        return formatter.format(new Date(System.currentTimeMillis()));
    }

    @Autowired
    public ItemDistributorService( List<PickUpStation> TestPickUpStations, LogManager logManager
    , @Lazy DeliveryEstimationService deliveryEstimationService, ConcurrentHashMap<String, Drone> drones) {

        this.logManager = logManager;
        this.pickUpStations = TestPickUpStations;
        this.deliveryEstimationService = deliveryEstimationService;
        this.drones = drones;
        // initialize();
    }

    public void addPriorityItems(List<ItemForDelivery> items) {
        if (items == null || items.isEmpty()) return;

        for (ItemForDelivery item : items) {
            if (item != null && item.getPickUpStation() != null) {
                ItemForDelivery.CurrentItemsAvailable.addItem(item);
                String[] logMessage = {
                    getHourWithAmPm(),
                    "Item Distributor Service",
                    "Added priority item: " + item.getItemName() + " at station: " + item.getPickUpStation().getName()
                };
                logManager.addLog(logMessage);
            }
        }
    }

    public void addPriorityItem(ItemForDelivery item) {
        if (item != null && item.getPickUpStation() != null) {
            ItemForDelivery.CurrentItemsAvailable.addItem(item);
            String[] logMessage = {
                getHourWithAmPm(),
                "Item Distributor Service",
                "Added priority item: " + item.getItemName() + " at station: " + item.getPickUpStation().getName()
            };
            logManager.addLog(logMessage);

            System.out.println("Added priority item: " + item.getItemName() + " at station: " + item.getPickUpStation().getName());
            ItemForDelivery.CurrentItemsAvailable.getItemsForDelivery();

            for(Drone drone : drones.values()) {
                int itemId = item.getItemIdInt();
                String droneId = drone.getDroneid();
                node destination = item.getTargetLocation();
                if(drone.getAvailable().equals(DroneStatus.AVAILABLE.getCode())) {
                deliveryEstimationService.updateEstimate(droneId, itemId, destination);
                }

                // Update the delivery estimation for the drone with the new item
        }
    }
    }


    // public void initializeItems(int amount, boolean extraItems) {
    //     if (ItemForDelivery.CurrentItemsAvailable.getItemsForDelivery().isEmpty()) {
    //         for (int i = 0; i < amount; i++) {
    //             String itemId = "Item" + i;
    //             ItemForDelivery item = new ItemForDelivery(itemId, pickUpStations.get(random.nextInt(pickUpStations.size())));
    //             ItemForDelivery.CurrentItemsAvailable.addItem(item);
    //         }
    //         String[] logMessage = {
    //             getHourWithAmPm(),
    //             "Item Distributor Service",
    //             "Items for delivery initialized with 14 items."
    //         };
    //         logManager.addLog(logMessage);
    //     }


    //    if(extraItems){
    //        ((java.util.concurrent.ScheduledExecutorService) itemAdderExecutor).scheduleAtFixedRate(() -> {
    //            int nextId = ItemForDelivery.CurrentItemsAvailable.getItemsForDelivery().size();
    //            String itemId = "Item" + nextId;

    //             PickUpStation randomStation = pickUpStations.get(random.nextInt(pickUpStations.size()));
    //             ItemForDelivery item = new ItemForDelivery(itemId, randomStation);
    //             ItemForDelivery.CurrentItemsAvailable.addItem(item);
    //             String[] logMessage = {
    //                 getHourWithAmPm(),
    //                 "Item Distributor Service",
    //                 "Added new item: " + item.getItemName()
    //             };

    //         }, 1, 1, java.util.concurrent.TimeUnit.SECONDS);
    //     }
    // }


    public PriorityBlockingQueue<ItemForDelivery> getItemsForDelivery() {
        return ItemForDelivery.CurrentItemsAvailable.getItemsForDelivery();
    }

    public void addDefinedItems(java.util.HashMap<String, java.util.Map<node, PickUpStation>> itemsMap) {
        System.out.println("addDefinedItems called with " + itemsMap.size() + " items");
        // if (itemsMap == null || itemsMap.isEmpty()) return;

        java.util.Map<Integer, Location> itemsMapInt = new java.util.HashMap<>();

        for (java.util.Map.Entry<String, java.util.Map<node, PickUpStation>> entry : itemsMap.entrySet()) {
            String itemId = entry.getKey();
            java.util.Map<node, PickUpStation> stationMap = entry.getValue();
            node location = stationMap.keySet().iterator().next();
            ItemForDelivery item = new ItemForDelivery(itemId, stationMap.values().iterator().next(), location);
            ItemForDelivery.CurrentItemsAvailable.addItem(item);

            itemsMapInt.put(item.getItemIdInt(), new Location(item.getTargetLocation()));

            String[] logMessage = {
                getHourWithAmPm(),
                "Item Distributor Service",
                "Added defined item: " + item.getItemName() + " at station: " + stationMap.values().iterator().next().getName()
            };
            logManager.addLog(logMessage);
            // for (Drone drone : drones.values()) {
            //     deliveryEstimationService.updateEstimate(drone.getDroneid(), item.getItemIdInt(), item.getTargetLocation());
            // }
        }

        deliveryEstimationService.updateAllEstimates(itemsMapInt);
    }
}


