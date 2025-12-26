package kr.hhplus.be.server.reservation.application.dataplatform;

import java.time.LocalDate;
import kr.hhplus.be.server.reservation.application.event.ReservationCompletedEvent;

public class ReservationDataPlatformPayload {
    private final Long reservationId;
    private final String userId;
    private final Long paymentId;
    private final long amount;
    private final LocalDate concertDate;
    private final int seatNumber;

    public ReservationDataPlatformPayload(ReservationCompletedEvent event) {
        this.reservationId = event.getReservationId();
        this.userId = event.getUserId();
        this.paymentId = event.getPaymentId();
        this.amount = event.getAmount();
        this.concertDate = event.getConcertDate();
        this.seatNumber = event.getSeatNumber();
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
