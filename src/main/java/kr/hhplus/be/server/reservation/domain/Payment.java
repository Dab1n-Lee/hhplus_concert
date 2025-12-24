package kr.hhplus.be.server.reservation.domain;

import java.time.LocalDateTime;

public class Payment {
    private Long id;
    private final Long reservationId;
    private final String userId;
    private final long amount;
    private final LocalDateTime paidAt;

    private Payment(Long id, Long reservationId, String userId, long amount, LocalDateTime paidAt) {
        this.id = id;
        this.reservationId = reservationId;
        this.userId = userId;
        this.amount = amount;
        this.paidAt = paidAt;
    }

    public static Payment create(Long reservationId, String userId, long amount, LocalDateTime paidAt) {
        return new Payment(null, reservationId, userId, amount, paidAt);
    }

    public static Payment rebuild(Long id, Long reservationId, String userId, long amount, LocalDateTime paidAt) {
        return new Payment(id, reservationId, userId, amount, paidAt);
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
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

    public LocalDateTime getPaidAt() {
        return paidAt;
    }
}
