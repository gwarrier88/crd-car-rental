package com.crd.rental.model;

import com.crd.rental.exception.InvalidReservationRequestException;

import java.time.LocalDateTime;

/**
 * Model record containing all the information required to reserve a car
 * @param type the car type to reserve
 * @param startDateTime pickup date and time
 * @param durationInDays number of 24-hour days from the pickup time, must be at least 1
 * @throws InvalidReservationRequestException if type or start is missing, or duration is less than 1
 */
public record ReservationRequest(CarType type, LocalDateTime startDateTime, int durationInDays) {

    public ReservationRequest {
        if(type == null || startDateTime == null)
            throw new InvalidReservationRequestException("A car type and start date are required");

        if(durationInDays <= 0)
            throw new InvalidReservationRequestException("Duration must be at least 1 day");
    }
}
