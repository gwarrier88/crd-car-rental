package com.crd.rental.model;

import java.time.LocalDateTime;

/**
 * Model record containing all the information required to reserve a car
 * @param type
 * @param startDateTime
 * @param durationInDays
 */
public record ReservationRequest(CarType type, LocalDateTime startDateTime, int durationInDays) {

    public ReservationRequest {
        if(type == null || startDateTime == null)
            throw new IllegalArgumentException("A car type and start date are required");

        if(durationInDays <= 0)
            throw new IllegalArgumentException("Duration must be at least 1 day");
    }
}
