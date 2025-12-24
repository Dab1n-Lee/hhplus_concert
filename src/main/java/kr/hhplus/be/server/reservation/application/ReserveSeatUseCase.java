package kr.hhplus.be.server.reservation.application;

import java.time.LocalDateTime;
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

    private final SeatPort seatPort;
    private final SeatReservationRepository reservationPort;
    private final ClockProvider clockProvider;

    public ReserveSeatUseCase(
        SeatPort seatPort,
        SeatReservationRepository reservationPort,
        ClockProvider clockProvider
    ) {
        this.seatPort = seatPort;
        this.reservationPort = reservationPort;
        this.clockProvider = clockProvider;
    }

    @Transactional
    public Reservation reserve(ReserveSeatCommand command) {
        if (command.getSeatNumber() < 1 || command.getSeatNumber() > MAX_SEAT_NUMBER) {
            throw new IllegalArgumentException("Seat number must be between 1 and 50.");
        }

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
