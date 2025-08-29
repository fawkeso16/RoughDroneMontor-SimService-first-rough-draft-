// Location Class - this class represents a location in the drone's navigation system, including its ID, name, and associated node. It provides methods to retrieve the coordinates of the node and to represent the location as a string.

package com;

public class Location {
    public String id;
    public String name;
    public node node;

    public Location(node location) {
        this.id = "temp";
        this.name = "temp";
        this.node = location;
    }


   public double getX() {
        return (int)this.node.x;
    }

    public double getY() {
        return (int)this.node.y;
    }

    public node getNode() {
        return this.node;
    }

    @Override
    public String toString() {
        return "Location{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", node=" + node +
                '}';
    }
}