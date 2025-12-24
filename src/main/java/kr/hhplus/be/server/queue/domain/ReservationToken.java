package kr.hhplus.be.server.queue.domain;

import java.time.LocalDateTime;

public class ReservationToken {
    private Long id;
    private final String userId;
    private final String token;
    private ReservationTokenStatus status;
    private final int position;
    private LocalDateTime expiresAt;
    private final LocalDateTime createdAt;

    private ReservationToken(
        Long id,
        String userId,
        String token,
        ReservationTokenStatus status,
        int position,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
    ) {
        this.id = id;
        this.userId = userId;
        this.token = token;
        this.status = status;
        this.position = position;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public static ReservationToken issue(
        String userId,
        String token,
        int position,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
    ) {
        return new ReservationToken(null, userId, token, ReservationTokenStatus.ACTIVE, position, expiresAt, createdAt);
    }

    public static ReservationToken rebuild(
        Long id,
        String userId,
        String token,
        ReservationTokenStatus status,
        int position,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
    ) {
        return new ReservationToken(id, userId, token, status, position, expiresAt, createdAt);
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt != null && expiresAt.isBefore(now);
    }

    public boolean isActiveOrWaiting() {
        return status == ReservationTokenStatus.ACTIVE || status == ReservationTokenStatus.WAITING;
    }

    public void expire(LocalDateTime expiredAt) {
        this.status = ReservationTokenStatus.EXPIRED;
        this.expiresAt = expiredAt;
    }

    public void complete() {
        this.status = ReservationTokenStatus.DONE;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getToken() {
        return token;
    }

    public ReservationTokenStatus getStatus() {
        return status;
    }

    public int getPosition() {
        return position;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
