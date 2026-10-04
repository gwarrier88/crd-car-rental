package com.crd.rental.model;

import java.time.LocalDateTime;

/**
 * Half-open time period record to keep track of a reservation
 * @param start
 * @param end
 */
public record ReservationPeriod(LocalDateTime start, LocalDateTime end) {

    public ReservationPeriod {
        if(start == null || end == null)
            throw new IllegalArgumentException("Start and end date should be specified");

        if(end.isBefore(start))
            throw new IllegalArgumentException("End date cannot be before start date");
    }

    /**
     * Check if a period overlaps with another one
     * @param other
     * @return
     */
    public boolean overlapsWith(ReservationPeriod other) {
        return start.isBefore(other.end) && end.isAfter(other.start);
    }
}
