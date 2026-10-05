package com.crd.rental.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class FleetTest {

    @Test
    void rejectsNegativeCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new Fleet(Map.of(CarType.SEDAN, 2, CarType.SUV, -1)));
    }

    @Test
    void acceptsZeroCapacity() {
        Fleet fleet = new Fleet(Map.of(CarType.VAN, 0));

        assertEquals(0, fleet.getCapacityOf(CarType.VAN));
    }

    @Test
    void returnsCapacityOfEachType() {
        Fleet fleet = new Fleet(Map.of(CarType.SEDAN, 3, CarType.SUV, 2, CarType.VAN, 1));

        assertEquals(3, fleet.getCapacityOf(CarType.SEDAN));
        assertEquals(2, fleet.getCapacityOf(CarType.SUV));
        assertEquals(1, fleet.getCapacityOf(CarType.VAN));
    }

    @Test
    void typeMissingFromFleetHasZeroCapacity() {
        Fleet fleet = new Fleet(Map.of(CarType.SEDAN, 3));

        assertEquals(0, fleet.getCapacityOf(CarType.SUV));
    }

    @Test
    void isNotChangedWhenSourceMapChanges() {
        Map<CarType, Integer> capacities = new HashMap<>(Map.of(CarType.SEDAN, 3));
        Fleet fleet = new Fleet(capacities);

        //changing the map after the fleet is created must not change the fleet
        capacities.put(CarType.SEDAN, 10);

        assertEquals(3, fleet.getCapacityOf(CarType.SEDAN));
    }

    @Test
    void rejectsNullCapacities() {
        assertThrows(IllegalArgumentException.class, () -> new Fleet(null));
    }

    @Test
    void rejectsNullCapacityForType() {
        //Map.of does not allow nulls, so use a HashMap
        Map<CarType, Integer> capacities = new HashMap<>();
        capacities.put(CarType.SEDAN, null);

        assertThrows(IllegalArgumentException.class, () -> new Fleet(capacities));
    }
}
