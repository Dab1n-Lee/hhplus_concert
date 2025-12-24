package kr.hhplus.be.server.reservation.controller;

import java.time.LocalDateTime;

public class ReservationResponse {
    private final Long reservationId;
    private final int seatNumber;
    private final LocalDateTime expiresAt;

    public ReservationResponse(Long reservationId, int seatNumber, LocalDateTime expiresAt) {
        this.reservationId = reservationId;
        this.seatNumber = seatNumber;
        this.expiresAt = expiresAt;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}
