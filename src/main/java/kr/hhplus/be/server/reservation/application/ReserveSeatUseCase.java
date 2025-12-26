package kr.hhplus.be.server.reservation.application;

import java.time.LocalDateTime;
import kr.hhplus.be.server.lock.adapter.redis.SpinDistributedLock;
import kr.hhplus.be.server.reservation.domain.Reservation;
import kr.hhplus.be.server.reservation.port.ClockProvider;
import kr.hhplus.be.server.reservation.port.SeatReservationRepository;
import kr.hhplus.be.server.reservation.port.SeatPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReserveSeatUseCase {
    private static final int MAX_SEAT_NUMBER = 50;
    private static final int HOLD_MINUTES = 5;
    private static final long LOCK_WAIT_TIME_MS = 1000; // 1 second
    private static final long LOCK_LEASE_TIME_MS = 5000; // 5 seconds

    private final SeatPort seatPort;
    private final SeatReservationRepository reservationPort;
    private final ClockProvider clockProvider;
    private final SpinDistributedLock distributedLock;

    public ReserveSeatUseCase(
        SeatPort seatPort,
        SeatReservationRepository reservationPort,
        ClockProvider clockProvider,
        SpinDistributedLock distributedLock
    ) {
        this.seatPort = seatPort;
        this.reservationPort = reservationPort;
        this.clockProvider = clockProvider;
        this.distributedLock = distributedLock;
    }

    public Reservation reserve(ReserveSeatCommand command) {
        if (command.getSeatNumber() < 1 || command.getSeatNumber() > MAX_SEAT_NUMBER) {
            throw new IllegalArgumentException("Seat number must be between 1 and 50.");
        }

        // Lock key: seat-specific lock to prevent concurrent reservation of the same seat
        String lockKey = String.format("seat:reserve:%s:%d", 
            command.getConcertDate().toString(), command.getSeatNumber());
        
        return distributedLock.executeWithLock(
            lockKey,
            LOCK_WAIT_TIME_MS,
            LOCK_LEASE_TIME_MS,
            () -> reserveInternal(command)
        );
    }

    @Transactional
    private Reservation reserveInternal(ReserveSeatCommand command) {
        LocalDateTime now = clockProvider.now();
        var seat = seatPort.loadForUpdate(command.getConcertDate(), command.getSeatNumber());

        if (seat.isHeld() && seat.isHoldExpired(now)) {
            seat.releaseHold();
            seatPort.save(seat);
            reservationPort.expireActiveBySeatId(seat.getId(), now);
        }

        if (!seat.isAvailable()) {
            throw new IllegalStateException("Seat is not available.");
        }

        LocalDateTime expiresAt = now.plusMinutes(HOLD_MINUTES);
        seat.hold(command.getUserId(), expiresAt);
        seatPort.save(seat);

        Reservation reservation = Reservation.hold(seat.getId(), command.getUserId(), expiresAt, now);
        return reservationPort.save(reservation);
    }
}
