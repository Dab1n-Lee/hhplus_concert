package kr.hhplus.be.server.reservation.controller;

public class PaymentRequest {
    private Long reservationId;
    private String userId;
    private long amount;

    public PaymentRequest() {
    }

    public PaymentRequest(Long reservationId, String userId, long amount) {
        this.reservationId = reservationId;
        this.userId = userId;
        this.amount = amount;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public String getUserId() {
        return userId;
    }

    public long getAmount() {
        return amount;
    }
}
