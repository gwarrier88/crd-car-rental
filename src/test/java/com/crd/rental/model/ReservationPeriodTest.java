package com.crd.rental.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class ReservationPeriodTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 10, 15, 10, 0, 0);
    //days are counted from START
    private static final ReservationPeriod DAYS_2_TO_5 = period(2, 5);

    private static ReservationPeriod period(int startDay, int endDay) {
        return new ReservationPeriod(START.plusDays(startDay), START.plusDays(endDay));
    }

    private static Stream<Arguments> invalidPeriods() {
        return Stream.of(
                Arguments.of(null, START),
                Arguments.of(START, null),
                Arguments.of(START, START.minusSeconds(1)),
                //zero length
                Arguments.of(START, START)
        );
    }

    @ParameterizedTest
    @MethodSource("invalidPeriods")
    void rejectsInvalidPeriods(LocalDateTime start, LocalDateTime end) {
        assertThrows(IllegalArgumentException.class, () -> new ReservationPeriod(start, end));
    }

    private static Stream<Arguments> overlapCases() {
        return Stream.of(
                Arguments.of(period(2, 5), true),   //identical
                Arguments.of(period(1, 3), true),   //overlaps the start
                Arguments.of(period(4, 6), true),   //overlaps the end
                Arguments.of(period(3, 4), true),   //inside
                Arguments.of(period(1, 6), true),   //contains
                Arguments.of(period(0, 2), false),  //ends when it starts (half-open)
                Arguments.of(period(5, 7), false),  //starts when it ends (half-open)
                Arguments.of(period(0, 1), false),  //before
                Arguments.of(period(6, 8), false)   //after
        );
    }

    @ParameterizedTest
    @MethodSource("overlapCases")
    void detectsOverlap(ReservationPeriod other, boolean expected) {
        //check both directions, overlap must not depend on which period is asked
        assertEquals(expected, DAYS_2_TO_5.overlapsWith(other));
        assertEquals(expected, other.overlapsWith(DAYS_2_TO_5));
    }
}
