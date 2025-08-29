package com;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class node {
    int x, y;
    
    @JsonIgnore  
    List<node> neighbors = new ArrayList<>();

    public node() {} 

    public node(int x, int y) {
        this.x = x;
        this.y = y;
    }

    
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public List<node> getNeighbors() {
        return neighbors;
    }

   
    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setNeighbors(List<node> neighbors) {
        this.neighbors = neighbors;
    }

    public double distanceTo(node other) {
        return Math.abs(this.x - other.x) + Math.abs(this.y - other.y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof node)) return false;
        node node = (node) o;
        return x == node.x && y == node.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "node{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}