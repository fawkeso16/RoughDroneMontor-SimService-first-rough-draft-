package com;

import java.util.List;

public class PathsUpdatedEvent {
    private final java.util.Map<String, List<node>> allPaths;

    public PathsUpdatedEvent(java.util.Map<String, List<node>> allPaths) {
        this.allPaths = allPaths;
    }

    public java.util. Map<String, List<node>> getAllPaths() {
        return allPaths;
    }
}