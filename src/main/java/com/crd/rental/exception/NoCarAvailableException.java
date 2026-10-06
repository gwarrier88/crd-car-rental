package com.crd.rental.exception;

/**
 * Thrown when every car of the requested type is reserved at some point in the requested period
 */
public class NoCarAvailableException extends RuntimeException {
    public NoCarAvailableException(String message) {
        super(message);
    }
}
