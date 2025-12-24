package kr.hhplus.be.server.reservation.controller;

import java.time.LocalDateTime;

public class PaymentResponse {
    private final Long paymentId;
    private final Long reservationId;
    private final long amount;
    private final LocalDateTime paidAt;

    public PaymentResponse(Long paymentId, Long reservationId, long amount, LocalDateTime paidAt) {
        this.paymentId = paymentId;
        this.reservationId = reservationId;
        this.amount = amount;
        this.paidAt = paidAt;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public long getAmount() {
        return amount;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }
}
