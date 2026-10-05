package com.crd.rental.model;

import java.util.Map;

/**
 * Number of cars of each type in the fleet. Capacities are fixed when the fleet is created.
 */
public class Fleet {
    private final Map<CarType, Integer> capacities;

    /**
     * @param initialCapacities number of cars of each type. Types left out have 0 cars.
     * @throws IllegalArgumentException if the map is null, or a type or capacity is null or negative
     */
    public Fleet(Map<CarType, Integer> initialCapacities) {
        if(initialCapacities == null)
            throw new IllegalArgumentException("Initial capacities must be specified");

        for(Map.Entry<CarType, Integer> entry : initialCapacities.entrySet()) {
            if(entry.getKey() == null || entry.getValue() == null)
                throw new IllegalArgumentException("Car type and capacity must be specified");

            if(entry.getValue() < 0)
                throw new IllegalArgumentException("Capacity cannot be negative");
        }

        this.capacities = Map.copyOf(initialCapacities);
    }

    /**
     * @param type the car type
     * @return number of cars of that type, or 0 if the type is not in the fleet
     */
    public int getCapacityOf(CarType type) {
        return this.capacities.getOrDefault(type, 0);
    }

}
