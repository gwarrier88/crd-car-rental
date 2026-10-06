package com.crd.rental.service;

import com.crd.rental.exception.InvalidReservationRequestException;
import com.crd.rental.exception.NoCarAvailableException;
import com.crd.rental.exception.ReservationNotFoundException;
import com.crd.rental.model.*;
import com.crd.rental.port.ReservationRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Reserves cars from a fixed fleet. A request is accepted only if, at every moment of the
 * requested period, the number of cars of that type already out is below the fleet capacity.
 */
public class ReservationService {

    private final Fleet fleet;
    private final ReservationRepository repository;
    private final Clock clock;

    /**
     * @param clock source of "now", injected so tests can fix the current time
     * @throws IllegalArgumentException if fleet, repository or clock is null
     */
    public ReservationService(Fleet fleet, ReservationRepository repository, Clock clock) {
        if(fleet == null)
            throw new IllegalArgumentException("Fleet must be specified");

        if(repository == null)
            throw new IllegalArgumentException("Repository must be specified");

        if(clock == null)
            throw new IllegalArgumentException("Clock must be specified");

        this.fleet = fleet;
        this.repository = repository;
        this.clock = clock;
    }

    public ReservationService(Fleet fleet, ReservationRepository repository) {
        this(fleet, repository, Clock.systemDefaultZone());
    }

    /**
     * Reserves a car of the requested type from the start time for the requested number of days.
     * @return the saved reservation
     * @throws InvalidReservationRequestException if the request is null or starts in the past
     * @throws NoCarAvailableException if every car of the type is out at some point in the period
     */
    public Reservation reserve(ReservationRequest request) {
        if(request == null)
            throw new InvalidReservationRequestException("Reservation request must be specified");

        LocalDateTime start = request.startDateTime();

        //check the start date is not in the past
        if(start.isBefore(LocalDateTime.now(clock)))
            throw new InvalidReservationRequestException("Start date cannot be in the past");

        LocalDateTime end = start.plusDays(request.durationInDays());
        ReservationPeriod requestPeriod = new ReservationPeriod(start, end);

        int totalCapacity = this.fleet.getCapacityOf(request.type());

        //lock by car type so only requests of that type wait and other types can continue
        synchronized (request.type()) {
            //find overlapping reservations for the requested type and period
            List<Reservation> overlappingReservations = this.repository.findActiveReservationsWithOverlap(request.type(), requestPeriod);

            //get the list of overlapping periods plus the requested one
            List<ReservationPeriod> overlappingPeriods = new ArrayList<>();
            for(Reservation r : overlappingReservations) {
                overlappingPeriods.add(r.getPeriod());
            }
            overlappingPeriods.add(requestPeriod);

            //check max cars out (cars reserved)
            int maxCarsOut = getMaxCarsOutForPeriods(overlappingPeriods);

            if(maxCarsOut > totalCapacity)
                throw new NoCarAvailableException("No cars of type " + request.type() + " available for requested period");

            Reservation newReservation = new Reservation(request.type(), requestPeriod);
            this.repository.save(newReservation);
            return newReservation;
        }
    }

    public Reservation findReservationById(UUID id) {
        return this.repository.findReservationById(id)
                .orElseThrow(() -> new ReservationNotFoundException("No reservation found with id " + id));
    }

    public List<Reservation> findReservationsByType(CarType type) {
        return this.repository.findReservationsByType(type);
    }

    /**
     * Returns the peak number of cars out at the same moment across the given periods.
     * Pickup and return times are sorted separately and walked in time order:
     * each pickup adds a car out, each return removes one. Sorting them separately is safe
     * because only the count matters, not which car comes back.
     * When a return and a pickup happen at the same moment, the return is counted first,
     * so a car returned at 10:00 can go out again at 10:00 (periods are [start, end)).
     *
     * @param periods reservation periods that overlap the requested period, plus the requested period
     * @return the maximum number of cars out at any single moment
     */
    private int getMaxCarsOutForPeriods(List<ReservationPeriod> periods) {
        int maxCarsOut = 0;
        int carsOut = 0;

        LocalDateTime[] pickupTimes = periods.stream()
                .map(ReservationPeriod::start)
                .sorted()
                .toArray(LocalDateTime[]::new);
        LocalDateTime[] returnTimes = periods.stream()
                .map(ReservationPeriod::end)
                .sorted()
                .toArray(LocalDateTime[]::new);

        int nextPickup = 0;
        int nextReturn = 0;

        while(nextPickup < pickupTimes.length) {
            if(pickupTimes[nextPickup].isBefore(returnTimes[nextReturn])) {
                carsOut++;
                if(carsOut > maxCarsOut)
                    maxCarsOut = carsOut;

                nextPickup++;
            } else {
              carsOut--;
              nextReturn++;
            }
        }

        return maxCarsOut;
    }
}
