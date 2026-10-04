package com.crd.rental.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Model entity responsible for keeping track of fleet asset capacities
 */
public class Fleet {
    private final Map<CarType, Integer> capacities = new ConcurrentHashMap<>();

    public void setCapacity(CarType type, int totalCapacity) {
        if(totalCapacity < 0) throw new IllegalArgumentException("Capacity cannot be negative");
        this.capacities.put(type, totalCapacity);
    }

    public int getCapacityOf(CarType type) {
        return this.capacities.getOrDefault(type, 0);
    }

}
