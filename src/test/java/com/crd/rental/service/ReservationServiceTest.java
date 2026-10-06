package com.crd.rental.service;

import com.crd.rental.adapter.InMemoryReservationRepository;
import com.crd.rental.exception.InvalidReservationRequestException;
import com.crd.rental.exception.NoCarAvailableException;
import com.crd.rental.exception.ReservationNotFoundException;
import com.crd.rental.model.CarType;
import com.crd.rental.model.Fleet;
import com.crd.rental.model.Reservation;
import com.crd.rental.model.ReservationPeriod;
import com.crd.rental.model.ReservationRequest;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class ReservationServiceTest {

    //fix the clock to set now so tests run in a deterministic way
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime START_DATE = LocalDateTime.of(2026, 10, 15, 10, 0, 0);

    private static ReservationService serviceWith(CarType type, int capacity) {
        return new ReservationService(new Fleet(Map.of(type, capacity)), new InMemoryReservationRepository(), CLOCK);
    }

    //days are counted from START_DATE, so reserve(service, SUV, 1, 4) reserves days 1-4
    private static Reservation reserve(ReservationService service, CarType type, int startDay, int endDay) {
        return service.reserve(new ReservationRequest(type, START_DATE.plusDays(startDay), endDay - startDay));
    }

    @Test
    void reservesCarOfRequestedTypeForRequestedDays() {
        ReservationService service = serviceWith(CarType.SEDAN, 3);

        //reserve a Sedan for 3 days
        Reservation reservation = service.reserve(new ReservationRequest(CarType.SEDAN, START_DATE, 3));

        assertEquals(1, service.findReservationsByType(CarType.SEDAN).size());
        assertEquals(CarType.SEDAN, reservation.getType());
        assertEquals(START_DATE, reservation.getPeriod().start());
        assertEquals(START_DATE.plusDays(3), reservation.getPeriod().end());
    }

    @Test
    void findsReservationById() {
        ReservationService service = serviceWith(CarType.SEDAN, 1);

        Reservation reservation = service.reserve(new ReservationRequest(CarType.SEDAN, START_DATE, 3));

        assertEquals(reservation, service.findReservationById(reservation.getId()));
    }

    @Test
    void throwsWhenReservationIdIsNotFound() {
        ReservationService service = serviceWith(CarType.SEDAN, 1);

        assertThrows(ReservationNotFoundException.class, () -> service.findReservationById(UUID.randomUUID()));
    }

    @Test
    void rejectsReservationWhenAllCarsOfTypeAreReserved() {
        ReservationService service = serviceWith(CarType.VAN, 2);

        //reserve full capacity
        service.reserve(new ReservationRequest(CarType.VAN, START_DATE, 3));
        service.reserve(new ReservationRequest(CarType.VAN, START_DATE, 3));

        //a new reservation of the same type throws an exception
        assertThrows(NoCarAvailableException.class, () -> service.reserve(new ReservationRequest(CarType.VAN, START_DATE, 2)));
    }

    @Test
    void rejectsPartialOverlapWhenFull() {
        ReservationService service = serviceWith(CarType.SUV, 1);

        reserve(service, CarType.SUV, 0, 3);

        //days 2-5 share only days 2-3 with the existing reservation, but the only SUV is out then
        assertThrows(NoCarAvailableException.class, () -> reserve(service, CarType.SUV, 2, 5));
    }

    @Test
    void typeWithNoCarsCannotBeReserved() {
        ReservationService service = serviceWith(CarType.VAN, 1);

        //fleet only has Vans, reserving an SUV throws an error
        assertThrows(NoCarAvailableException.class, () -> service.reserve(new ReservationRequest(CarType.SUV, START_DATE, 2)));
    }

    @Test
    void typeWithZeroCapacityCannotBeReserved() {
        ReservationService service = serviceWith(CarType.VAN, 0);

        assertThrows(NoCarAvailableException.class, () -> service.reserve(new ReservationRequest(CarType.VAN, START_DATE, 2)));
    }

    @Test
    void fullTypeDoesNotBlockOtherTypes() {
        ReservationService service = new ReservationService(
                new Fleet(Map.of(CarType.VAN, 1, CarType.SEDAN, 1)), new InMemoryReservationRepository(), CLOCK);

        //the only Van is reserved
        reserve(service, CarType.VAN, 0, 3);

        //a Sedan for the same days is still available
        assertDoesNotThrow(() -> reserve(service, CarType.SEDAN, 0, 3));
    }

    @Test
    void rejectedRequestIsNotSaved() {
        ReservationService service = serviceWith(CarType.SUV, 1);
        reserve(service, CarType.SUV, 0, 3);

        assertThrows(NoCarAvailableException.class, () -> reserve(service, CarType.SUV, 1, 2));

        assertEquals(1, service.findReservationsByType(CarType.SUV).size());
    }

    @Test
    void carsCanBeReservedBackToBack() {
        ReservationService service = serviceWith(CarType.SUV, 1);

        //reserve an SUV for 3 days
        Reservation first = service.reserve(new ReservationRequest(CarType.SUV, START_DATE, 3));

        //reserve an SUV with a start date when the first one ends (proves half-open periods)
        //no exceptions are thrown
        assertDoesNotThrow(() -> service.reserve(new ReservationRequest(CarType.SUV, first.getPeriod().end(), 2)));
    }

    @Test
    void acceptsReservationEndingWhenExistingOneStarts() {
        ReservationService service = serviceWith(CarType.SUV, 1);

        reserve(service, CarType.SUV, 3, 5);

        //days 1-3 end exactly when the existing reservation starts
        assertDoesNotThrow(() -> reserve(service, CarType.SUV, 1, 3));
    }

    @Test
    void acceptsRequestOverlappingTwoReservationsThatDoNotOverlapEachOther() {
        ReservationService service = serviceWith(CarType.SUV, 2);

        reserve(service, CarType.SUV, 0, 2);
        reserve(service, CarType.SUV, 3, 5);

        //days 1-5 overlap both, but at most 2 SUVs are ever out at once
        assertDoesNotThrow(() -> reserve(service, CarType.SUV, 1, 5));
    }

    @Test
    void rejectsRequestOverlappingTwoSeparateReservationsWhenOnlyOneCar() {
        ReservationService service = serviceWith(CarType.SUV, 1);

        reserve(service, CarType.SUV, 0, 2);
        reserve(service, CarType.SUV, 3, 5);

        //with a single SUV, days 1-4 clash with both reservations
        assertThrows(NoCarAvailableException.class, () -> reserve(service, CarType.SUV, 1, 4));
    }

    @Test
    void carCanBeReservedWhenItOnlyFitsByReassigningCars() {
        ReservationService service = serviceWith(CarType.SEDAN, 2);

        reserve(service, CarType.SEDAN, 2, 4);
        reserve(service, CarType.SEDAN, 6, 8);
        reserve(service, CarType.SEDAN, 3, 5);

        //days 4-7 fit only if the car returned on day 4 takes this reservation and the other car
        //(free from day 5) takes the day 6-8 reservation; at most 2 Sedans are out at once
        assertDoesNotThrow(() -> reserve(service, CarType.SEDAN, 4, 7));
    }

    @Test
    void rejectsWhenPeakInsideRequestExceedsCapacity() {
        ReservationService service = serviceWith(CarType.SEDAN, 2);

        reserve(service, CarType.SEDAN, 0, 4);
        reserve(service, CarType.SEDAN, 2, 6);

        //on day 3 both Sedans are already out, so a third reservation that day cannot fit
        assertThrows(NoCarAvailableException.class, () -> reserve(service, CarType.SEDAN, 3, 5));
    }

    @Test
    void rejectsNullRequest() {
        ReservationService service = serviceWith(CarType.SEDAN, 1);

        assertThrows(InvalidReservationRequestException.class, () -> service.reserve(null));
    }

    @Test
    void rejectsRequestWithStartDateInThePast() {
        ReservationService service = serviceWith(CarType.SEDAN, 1);

        //one second before the fixed clock's now
        LocalDateTime past = LocalDateTime.now(CLOCK).minusSeconds(1);

        assertThrows(InvalidReservationRequestException.class, () -> service.reserve(new ReservationRequest(CarType.SEDAN, past, 2)));
    }

    @Test
    void acceptsRequestStartingNow() {
        ReservationService service = serviceWith(CarType.SEDAN, 1);

        //start date equal to now is not in the past
        assertDoesNotThrow(() -> service.reserve(new ReservationRequest(CarType.SEDAN, LocalDateTime.now(CLOCK), 2)));
    }

    @Test
    void rejectsNullConstructorArguments() {
        Fleet fleet = new Fleet(Map.of(CarType.SEDAN, 1));
        InMemoryReservationRepository repository = new InMemoryReservationRepository();

        assertThrows(IllegalArgumentException.class, () -> new ReservationService(null, repository, CLOCK));
        assertThrows(IllegalArgumentException.class, () -> new ReservationService(fleet, null, CLOCK));
        assertThrows(IllegalArgumentException.class, () -> new ReservationService(fleet, repository, null));
    }

    @Test
    void onlyCapacityManyConcurrentRequestsSucceed() throws Exception {
        int capacity = 3;
        int threads = 20;

        //pause between reading the overlapping reservations and saving the new one,
        //so without a lock every thread would see no reservations and all would succeed
        InMemoryReservationRepository slowRepository = new InMemoryReservationRepository() {
            @Override
            public List<Reservation> findActiveReservationsWithOverlap(CarType type, ReservationPeriod period) {
                List<Reservation> overlapping = super.findActiveReservationsWithOverlap(type, period);
                try {
                    Thread.sleep(10);
                } catch(InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return overlapping;
            }
        };
        ReservationService service = new ReservationService(new Fleet(Map.of(CarType.SUV, capacity)), slowRepository, CLOCK);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        //every thread waits on the latch so all requests for the same period arrive at the same moment
        for(int i = 0; i < threads; i++) {
            results.add(executor.submit(() -> {
                start.await();
                try {
                    reserve(service, CarType.SUV, 0, 3);
                    return true;
                } catch(NoCarAvailableException e) {
                    return false;
                }
            }));
        }
        start.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        //get() rethrows any other exception a thread hit, failing the test
        int succeeded = 0;
        for(Future<Boolean> result : results) {
            if(result.get())
                succeeded++;
        }

        assertEquals(capacity, succeeded);
        assertEquals(capacity, service.findReservationsByType(CarType.SUV).size());
    }
}
