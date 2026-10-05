package com.crd.rental.model;

import java.time.LocalDateTime;

/**
 * Time period of a reservation, half-open: [start, end).
 * The car is out from start up to, but not including, end, so a car returned at 10:00
 * can be picked up again at 10:00.
 * @param start pickup time, included in the period
 * @param end return time, not included in the period; must be after start
 */
public record ReservationPeriod(LocalDateTime start, LocalDateTime end) {

    public ReservationPeriod {
        if(start == null || end == null)
            throw new IllegalArgumentException("Start and end date should be specified");

        if(end.isBefore(start))
            throw new IllegalArgumentException("End date cannot be before start date");

        if(!end.isAfter(start))
            throw new IllegalArgumentException("End date must be after start date");
    }

    /**
     * @param other the period to compare with
     * @return true if the two periods share any moment; periods that only touch at an end do not overlap
     */
    public boolean overlapsWith(ReservationPeriod other) {
        return start.isBefore(other.end) && end.isAfter(other.start);
    }
}
