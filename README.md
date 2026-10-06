# Car Rental Reservation System

A simulated car rental system in plain Java. A customer can reserve a car of a given type
(Sedan, SUV or Van) starting at a date and time, for a number of days. Each car type has a
limited number of cars, and the system rejects a reservation when no car of that type is free
for the whole requested period.

## Requirements

Given:

1. Reserve a car of a given type at a desired date and time for a given number of days.
2. There are 3 types of cars: sedan, SUV and van.
3. The number of cars of each type is limited.
4. Use unit tests to prove the system satisfies the requirements.

Tests for each requirement:

- Reserving a type for a number of days: ReservationServiceTest, ReservationRequestTest
- Limited cars per type: ReservationServiceTest, ReservationPeriodTest, FleetTest

To build and run:

- Java 21
- Maven does not need to be installed. The Maven Wrapper (`mvnw`) downloads it on first run.

## How to run

Run all tests:

```bash
./mvnw test
```

Run one test class:

```bash
./mvnw test -Dtest=ReservationServiceTest
```

On Windows, use `mvnw.cmd` instead of `./mvnw`.

## Assumptions

- A day is 24 hours from the start time. A 3-day reservation starting at 10:00 on the 15th ends
  at 10:00 on the 18th.
- A reservation must start now or in the future and last at least 1 day.
- A customer reserves a car type, not a specific car. Cars of the same type are interchangeable.
- A car returned at 10:00 can be reserved again from 10:00. Reservation periods include the
  start time but not the end time.
- The number of cars of each type is set when the fleet is created and does not change. A type
  not in the fleet has 0 cars.
- Times use the system's default time zone. Time zones and daylight saving are not handled.
- Reservations are kept in memory only.
- Cancelling or changing a reservation, customers and pricing are out of scope.

## Design choices

Code is under `src/main/java/com/crd/rental`:

- `model` - car types, the fleet, reservations and reservation requests
- `service` - `ReservationService`, which validates requests and checks availability
- `port` - `ReservationRepository`, the storage interface the service uses
- `adapter` - `InMemoryReservationRepository`, the in-memory implementation
- `exception` - exceptions for invalid requests, no car available and reservation not found

Availability is checked by counting cars, not by assigning them. For a new request, the
service loads the reservations of that type that overlap the requested period, adds the
request, and finds the most cars out at the same moment. The request is accepted if that number
does not exceed the number of cars of that type. This accepts a request that only fits if
existing reservations move between cars: with 2 sedans reserved for days 2-4, 3-5 and 6-8, a
request for days 4-7 is accepted because no more than 2 sedans are ever out at once.

The service depends on the `ReservationRepository` interface rather than the in-memory class, so
the storage could be replaced, for example with a database, without changing the reservation
logic.

Checking availability and saving the reservation happen inside a lock on the car type, so two
requests for the last car of a type cannot both succeed. Requests for different types do not
wait for each other. This works within a single process; with a database the locking would move
to the database. Concurrency was not part of the requirements, but reserving the last car twice
is the main risk in a reservation system, so it is handled and tested.

The service takes a `Clock` so tests can fix the current time and give the same result on every
run.
