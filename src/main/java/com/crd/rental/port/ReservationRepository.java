package com.crd.rental.port;

import com.crd.rental.model.CarType;
import com.crd.rental.model.Reservation;
import com.crd.rental.model.ReservationPeriod;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Storage for reservations. The service depends on this interface, not on a specific store.
 */
public interface ReservationRepository {

    void save(Reservation reservation);

    /**
     * @return all reservations of the type, past and future. The list cannot be modified.
     */
    List<Reservation> findReservationsByType(CarType type);

    /**
     * @return reservations of the type whose period overlaps the given one.
     *         Periods are half-open, so a reservation ending when the period starts is not included.
     */
    List<Reservation> findActiveReservationsWithOverlap(CarType type, ReservationPeriod period);

    Optional<Reservation> findReservationById(UUID id);
}
