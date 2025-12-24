package kr.hhplus.be.server.reservation.application;

import java.time.LocalDateTime;
import kr.hhplus.be.server.reservation.domain.Payment;
import kr.hhplus.be.server.reservation.domain.ReservationStatus;
import kr.hhplus.be.server.reservation.port.ClockProvider;
import kr.hhplus.be.server.reservation.port.NotificationPort;
import kr.hhplus.be.server.reservation.port.PaymentPort;
import kr.hhplus.be.server.reservation.port.PointPort;
import kr.hhplus.be.server.reservation.port.ReservationPort;
import kr.hhplus.be.server.reservation.port.SeatPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayReservationUseCase {
    private final ReservationPort reservationPort;
    private final SeatPort seatPort;
    private final PointPort pointPort;
    private final PaymentPort paymentPort;
    private final NotificationPort notificationPort;
    private final ClockProvider clockProvider;

    public PayReservationUseCase(
        ReservationPort reservationPort,
        SeatPort seatPort,
        PointPort pointPort,
        PaymentPort paymentPort,
        NotificationPort notificationPort,
        ClockProvider clockProvider
    ) {
        this.reservationPort = reservationPort;
        this.seatPort = seatPort;
        this.pointPort = pointPort;
        this.paymentPort = paymentPort;
        this.notificationPort = notificationPort;
        this.clockProvider = clockProvider;
    }

    @Transactional
    public Payment pay(PayReservationCommand command) {
        LocalDateTime now = clockProvider.now();
        var reservation = reservationPort.loadForUpdate(command.getReservationId());

        if (!reservation.getUserId().equals(command.getUserId())) {
            throw new IllegalArgumentException("Reservation does not belong to user.");
        }

        if (reservation.getStatus() != ReservationStatus.HOLD) {
            throw new IllegalStateException("Reservation is not in hold status.");
        }

        if (reservation.isExpired(now)) {
            reservation.expire(now);
            reservationPort.save(reservation);
            var seat = seatPort.loadForUpdate(reservation.getSeatId());
            seat.releaseHold();
            seatPort.save(seat);
            throw new IllegalStateException("Reservation expired.");
        }

        var seat = seatPort.loadForUpdate(reservation.getSeatId());
        if (!seat.isHeldBy(command.getUserId())) {
            throw new IllegalStateException("Seat is not held by user.");
        }

        if (pointPort.getBalance(command.getUserId()) < command.getAmount()) {
            throw new IllegalStateException("Insufficient points.");
        }

        pointPort.use(command.getUserId(), command.getAmount());
        seat.reserve(command.getUserId());
        seatPort.save(seat);

        reservation.confirm(now);
        reservationPort.save(reservation);

        Payment payment = paymentPort.save(
            Payment.create(reservation.getId(), command.getUserId(), command.getAmount(), now)
        );
        notificationPort.sendReservationConfirmed(payment.getReservationId(), command.getUserId(), seat.getSeatNumber());
        return payment;
    }
}
