package com;

import java.util.List;

public class DoubleJob extends Job {
    private final ItemForDelivery secondItem;
    private final Location secondDestination;
    private final List<node> pathFromFirstTargetToSecondTarget;
    

    public DoubleJob(String id, ItemForDelivery firstItem, Drone drone, Location firstDestination,
                     ItemForDelivery secondItem, Location secondDestination,
                     List<node> pathToPickupFirst, List<node> pathFromPickupToFirstTarget,
                     List<node> pathFromFirstTargetToSecondTarget) {
        super(id, firstItem, drone, firstDestination, pathToPickupFirst, pathFromPickupToFirstTarget);
        this.secondItem = secondItem;
        this.secondDestination = secondDestination;
        this.pathFromFirstTargetToSecondTarget = pathFromFirstTargetToSecondTarget;
        appendToFullPath(pathFromFirstTargetToSecondTarget);

    }

    public ItemForDelivery getSecondItem() {
        return secondItem;
    }

    public Location getSecondDestination() {
        return secondDestination;
    }

    public List<node> getPathFromFirstTargetToSecondTarget() {
        return pathFromFirstTargetToSecondTarget;
    }



}
