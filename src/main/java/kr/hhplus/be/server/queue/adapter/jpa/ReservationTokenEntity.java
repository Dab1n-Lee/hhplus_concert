package kr.hhplus.be.server.queue.adapter.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import kr.hhplus.be.server.queue.domain.ReservationTokenStatus;

@Entity
@Table(
    name = "reservation_token",
    indexes = {
        @Index(name = "idx_reservation_token_user", columnList = "user_id"),
        @Index(name = "idx_reservation_token_token", columnList = "token")
    }
)
public class ReservationTokenEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReservationTokenStatus status;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected ReservationTokenEntity() {
    }

    public ReservationTokenEntity(
        String userId,
        String token,
        ReservationTokenStatus status,
        int position,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
    ) {
        this.userId = userId;
        this.token = token;
        this.status = status;
        this.position = position;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public void updateStatus(ReservationTokenStatus status, LocalDateTime expiresAt) {
        this.status = status;
        this.expiresAt = expiresAt;
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
