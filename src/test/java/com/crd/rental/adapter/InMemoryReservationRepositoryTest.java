package com.crd.rental.adapter;

import com.crd.rental.model.CarType;
import com.crd.rental.model.Reservation;
import com.crd.rental.model.ReservationPeriod;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class InMemoryReservationRepositoryTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 10, 5, 9, 0, 0);
    private static final ReservationPeriod PERIOD = new ReservationPeriod(START, START.plusDays(1));

    @Test
    void savesReservationUnderItsType() {
        Reservation r = new Reservation(CarType.SEDAN, PERIOD);

        InMemoryReservationRepository inMemoryReservationRepository = new InMemoryReservationRepository();
        inMemoryReservationRepository.save(r);

        assertEquals(1, inMemoryReservationRepository.findReservationsByType(CarType.SEDAN).size());
        assertEquals(r, inMemoryReservationRepository.findReservationsByType(CarType.SEDAN).getFirst());
    }

    @Test
    void returnsOnlyOverlappingReservationsOfRequestedType() {
        ReservationPeriod period1 = new ReservationPeriod(START, START.plusDays(2));
        ReservationPeriod period2 = new ReservationPeriod(START.plusDays(1), START.plusDays(2));

        Reservation r1 = new Reservation(CarType.SEDAN, period1);
        Reservation r2 = new Reservation(CarType.SEDAN, period2);
        Reservation r3 = new Reservation(CarType.SUV, period1);

        InMemoryReservationRepository inMemoryReservationRepository = new InMemoryReservationRepository();
        inMemoryReservationRepository.save(r1);
        inMemoryReservationRepository.save(r2);
        inMemoryReservationRepository.save(r3);

        assertEquals(2, inMemoryReservationRepository.findReservationsByType(CarType.SEDAN).size());
        assertEquals(1, inMemoryReservationRepository.findReservationsByType(CarType.SUV).size());

        ReservationPeriod newPeriod = new ReservationPeriod(START.plusHours(6), START.plusDays(2).minusHours(1));

        List<Reservation> overlappingReservations = inMemoryReservationRepository.findActiveReservationsWithOverlap(CarType.SEDAN, newPeriod);
        assertEquals(2, overlappingReservations.size());
        assertEquals(r1, overlappingReservations.getFirst());
        assertEquals(r2, overlappingReservations.get(1));
    }

    @Test
    void excludesReservationsThatDoNotOverlap() {
        InMemoryReservationRepository repository = new InMemoryReservationRepository();

        //ends exactly when the requested period starts (half-open, so no overlap)
        repository.save(new Reservation(CarType.SEDAN, new ReservationPeriod(START.minusDays(2), START)));
        //starts exactly when the requested period ends
        repository.save(new Reservation(CarType.SEDAN, new ReservationPeriod(START.plusDays(1), START.plusDays(3))));
        //well after the requested period
        repository.save(new Reservation(CarType.SEDAN, new ReservationPeriod(START.plusDays(5), START.plusDays(6))));

        assertTrue(repository.findActiveReservationsWithOverlap(CarType.SEDAN, PERIOD).isEmpty());
    }

    @Test
    void returnsEmptyListForTypeWithNoReservations() {
        InMemoryReservationRepository repository = new InMemoryReservationRepository();

        assertTrue(repository.findReservationsByType(CarType.VAN).isEmpty());
    }

    @Test
    void returnedReservationsCannotBeModified() {
        InMemoryReservationRepository repository = new InMemoryReservationRepository();
        repository.save(new Reservation(CarType.SEDAN, PERIOD));

        assertThrows(UnsupportedOperationException.class,
                () -> repository.findReservationsByType(CarType.SEDAN).clear());
    }

    @Test
    void findsAReservationById() {
        Reservation r = new Reservation(CarType.SUV, PERIOD);

        InMemoryReservationRepository inMemoryReservationRepository = new InMemoryReservationRepository();
        inMemoryReservationRepository.save(r);

        assertEquals(r, inMemoryReservationRepository.findReservationById(r.getId()).orElseThrow());
    }

    @Test
    void returnsEmptyWhenIdIsNotFound() {
        InMemoryReservationRepository repository = new InMemoryReservationRepository();
        repository.save(new Reservation(CarType.SEDAN, PERIOD));

        assertTrue(repository.findReservationById(UUID.randomUUID()).isEmpty());
    }
}
