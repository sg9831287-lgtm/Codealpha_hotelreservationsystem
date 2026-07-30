# Hotel Reservation System

A production-ready Java console application implementing a layered hotel booking system.

## Features
- Search available rooms by category (Standard / Deluxe / Suite)
- Make new reservations with unique Booking IDs
- Cancel reservations with automatic refund simulation
- View detailed booking receipts
- Persistent File I/O — data survives application restarts

## Architecture
```
com.hotel/
├── domain/       → Room, Reservation, User, enums
├── exception/    → RoomUnavailableException, InvalidBookingIdException, PaymentException
├── repository/   → FileRepository interface, RoomRepository, ReservationRepository
├── service/      → BookingService, PaymentProcessor
└── ui/           → ConsoleUI (CLI presentation layer)
```

## How to Compile & Run

```bash
# From the project root
javac -d out -sourcepath src/main/java $(find src -name "*.java")
java -cp out com.hotel.Main
```

> On Windows PowerShell:
> ```powershell
> Get-ChildItem -Recurse -Filter "*.java" src | ForEach-Object { $_.FullName } | Set-Content sources.txt
> javac -d out @sources.txt
> java -cp out com.hotel.Main
> ```

## Room Inventory (seeded on first launch)
| Room | Type     | Floor | Rate/Night |
|------|----------|-------|------------|
| 101–105 | Standard | 1  | $99.00     |
| 201–203 | Deluxe   | 2  | $179.00    |
| 301–302 | Suite    | 3  | $349.00    |

## Tech Stack
- **Language:** Java 17+
- **Persistence:** Java Object Serialization (`.dat` files)
- **Concurrency:** `ReentrantReadWriteLock` per repository
- **Logging:** Java Util Logging (JUL)
