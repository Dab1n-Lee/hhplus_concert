package kr.hhplus.be.server.reservation.application.event;

import java.time.LocalDate;

public class ReservationCompletedEvent {
    private final Long reservationId;
    private final String userId;
    private final Long paymentId;
    private final long amount;
    private final LocalDate concertDate;
    private final int seatNumber;

    public ReservationCompletedEvent(
        Long reservationId,
        String userId,
        Long paymentId,
        long amount,
        LocalDate concertDate,
        int seatNumber
    ) {
        this.reservationId = reservationId;
        this.userId = userId;
        this.paymentId = paymentId;
        this.amount = amount;
        this.concertDate = concertDate;
        this.seatNumber = seatNumber;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public String getUserId() {
        return userId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public long getAmount() {
        return amount;
    }

    public LocalDate getConcertDate() {
        return concertDate;
    }

    public int getSeatNumber() {
        return seatNumber;
    }
}
