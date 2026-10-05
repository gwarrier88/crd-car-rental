package com.crd.rental.model;

import com.crd.rental.exception.InvalidReservationRequestException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ReservationRequestTest {

    private static final LocalDateTime START_DATE = LocalDateTime.of(2026, 10, 15, 10, 0, 0);

    //pass raw values, the request is validated in its constructor so it can't be built in the source
    private static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of(null, START_DATE, 2),
                Arguments.of(CarType.SEDAN, null, 2),
                Arguments.of(CarType.SUV, START_DATE, 0),
                Arguments.of(CarType.SUV, START_DATE, -1)
        );
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void rejectsInvalidRequests(CarType type, LocalDateTime start, int days) {
        assertThrows(InvalidReservationRequestException.class, () -> new ReservationRequest(type, start, days));
    }
}
