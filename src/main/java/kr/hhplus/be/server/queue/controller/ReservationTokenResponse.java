package kr.hhplus.be.server.queue.controller;

import java.time.LocalDateTime;
import kr.hhplus.be.server.queue.domain.ReservationTokenStatus;

public class ReservationTokenResponse {
    private final String token;
    private final ReservationTokenStatus status;
    private final int position;
    private final LocalDateTime expiresAt;

    public ReservationTokenResponse(
        String token,
        ReservationTokenStatus status,
        int position,
        LocalDateTime expiresAt
    ) {
        this.token = token;
        this.status = status;
        this.position = position;
        this.expiresAt = expiresAt;
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
}
