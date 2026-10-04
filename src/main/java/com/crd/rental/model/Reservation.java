package com.crd.rental.model;

import java.util.UUID;

/**
 * Model entity object to keep track of a reservation
 */
public class Reservation {

    private final UUID id;
    private final CarType type;
    private final ReservationPeriod period;

    public Reservation(CarType type, ReservationPeriod period) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.period = period;
    }

    public UUID getId() {
        return id;
    }

    public CarType getType() {
        return type;
    }

    public ReservationPeriod getPeriod() {
        return period;
    }
}
