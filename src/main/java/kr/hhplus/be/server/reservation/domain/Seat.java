package kr.hhplus.be.server.reservation.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Seat {
    private final Long id;
    private final LocalDate concertDate;
    private final int seatNumber;
    private SeatStatus status;
    private String holdUserId;
    private LocalDateTime holdExpiresAt;
    private String reservedUserId;

    public Seat(
        Long id,
        LocalDate concertDate,
        int seatNumber,
        SeatStatus status,
        String holdUserId,
        LocalDateTime holdExpiresAt,
        String reservedUserId
    ) {
        this.id = id;
        this.concertDate = concertDate;
        this.seatNumber = seatNumber;
        this.status = status;
        this.holdUserId = holdUserId;
        this.holdExpiresAt = holdExpiresAt;
        this.reservedUserId = reservedUserId;
    }

    public boolean isAvailable() {
        return status == SeatStatus.AVAILABLE;
    }

    public boolean isHeld() {
        return status == SeatStatus.HELD;
    }

    public boolean isHeldBy(String userId) {
        return status == SeatStatus.HELD && userId != null && userId.equals(holdUserId);
    }

    public boolean isHoldExpired(LocalDateTime now) {
        return holdExpiresAt != null && holdExpiresAt.isBefore(now);
    }

    public void hold(String userId, LocalDateTime expiresAt) {
        this.status = SeatStatus.HELD;
        this.holdUserId = userId;
        this.holdExpiresAt = expiresAt;
    }

    public void releaseHold() {
        this.status = SeatStatus.AVAILABLE;
        this.holdUserId = null;
        this.holdExpiresAt = null;
    }

    public void reserve(String userId) {
        this.status = SeatStatus.RESERVED;
        this.reservedUserId = userId;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getConcertDate() {
        return concertDate;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public String getHoldUserId() {
        return holdUserId;
    }

    public LocalDateTime getHoldExpiresAt() {
        return holdExpiresAt;
    }

    public String getReservedUserId() {
        return reservedUserId;
    }
}
