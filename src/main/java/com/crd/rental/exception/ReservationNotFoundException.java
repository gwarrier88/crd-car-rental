package com.crd.rental.exception;

/**
 * Thrown when no reservation exists with the given id
 */
public class ReservationNotFoundException extends RuntimeException {
    public ReservationNotFoundException(String message) {
        super(message);
    }
}
