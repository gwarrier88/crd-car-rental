package com.crd.rental.exception;

/**
 * Thrown when a reservation request is missing data or breaks a reservation rule
 */
public class InvalidReservationRequestException extends RuntimeException {
    public InvalidReservationRequestException(String message) {
        super(message);
    }
}
