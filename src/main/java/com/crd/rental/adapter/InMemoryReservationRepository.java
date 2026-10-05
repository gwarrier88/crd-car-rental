package com.crd.rental.adapter;

import com.crd.rental.model.CarType;
import com.crd.rental.model.Reservation;
import com.crd.rental.model.ReservationPeriod;
import com.crd.rental.port.ReservationRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe in-memory implementation of the reservation repository.
 * Each call is safe on its own. Reads see a snapshot, so they never fail while a save is in progress.
 * The check-then-save in ReservationService is not atomic here; the service locks per car type for that.
 */
public class InMemoryReservationRepository implements ReservationRepository {

    private final Map<CarType, List<Reservation>> reservations = new ConcurrentHashMap<>();

    @Override
    public void save(Reservation reservation) {
        reservations.computeIfAbsent(reservation.getType(), type -> new CopyOnWriteArrayList<>()).add(reservation);
    }

    @Override
    public List<Reservation> findReservationsByType(CarType type) {
        return List.copyOf(reservations.getOrDefault(type, List.of()));
    }

    @Override
    public List<Reservation> findActiveReservationsWithOverlap(CarType type, ReservationPeriod period) {
        return findReservationsByType(type)
                .stream()
                .filter(r -> r.getPeriod().overlapsWith(period))
                .toList();
    }

    @Override
    public Optional<Reservation> findReservationById(UUID id) {
        return reservations.values().stream()
                .flatMap(List::stream)
                .filter(r -> r.getId().equals(id))
                .findFirst();
    }
}
