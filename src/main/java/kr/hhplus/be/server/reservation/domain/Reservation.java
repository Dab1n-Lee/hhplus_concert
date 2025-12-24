package kr.hhplus.be.server.reservation.domain;

import java.time.LocalDateTime;

public class Reservation {
    private Long id;
    private final Long seatId;
    private final String userId;
    private ReservationStatus status;
    private LocalDateTime expiresAt;
    private final LocalDateTime createdAt;

    private Reservation(
        Long id,
        Long seatId,
        String userId,
        ReservationStatus status,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
    ) {
        this.id = id;
        this.seatId = seatId;
        this.userId = userId;
        this.status = status;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public static Reservation hold(Long seatId, String userId, LocalDateTime expiresAt, LocalDateTime createdAt) {
        return new Reservation(null, seatId, userId, ReservationStatus.HOLD, expiresAt, createdAt);
    }

    public static Reservation rebuild(
        Long id,
        Long seatId,
        String userId,
        ReservationStatus status,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
    ) {
        return new Reservation(id, seatId, userId, status, expiresAt, createdAt);
    }

    public void confirm(LocalDateTime confirmedAt) {
        this.status = ReservationStatus.CONFIRMED;
        this.expiresAt = confirmedAt;
    }

    public void expire(LocalDateTime expiredAt) {
        this.status = ReservationStatus.EXPIRED;
        this.expiresAt = expiredAt;
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt != null && expiresAt.isBefore(now);
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Long getSeatId() {
        return seatId;
    }

    public String getUserId() {
        return userId;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
