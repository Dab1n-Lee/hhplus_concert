package kr.hhplus.be.server.concert.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "seat",
    uniqueConstraints = @UniqueConstraint(columnNames = {"concert_date_id", "seat_number"})
)
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "concert_date_id", nullable = false)
    private ConcertDate concertDate;

    @Column(name = "seat_number", nullable = false)
    private int seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SeatStatus status;

    @Column(name = "hold_user_id")
    private String holdUserId;

    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;

    @Column(name = "reserved_user_id")
    private String reservedUserId;

    protected Seat() {
    }

    private Seat(ConcertDate concertDate, int seatNumber, SeatStatus status) {
        this.concertDate = concertDate;
        this.seatNumber = seatNumber;
        this.status = status;
    }

    public static Seat available(ConcertDate concertDate, int seatNumber) {
        return new Seat(concertDate, seatNumber, SeatStatus.AVAILABLE);
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
        this.reservedUserId = null;
    }

    public void reserve(String userId) {
        this.status = SeatStatus.RESERVED;
        this.reservedUserId = userId;
        this.holdUserId = null;
        this.holdExpiresAt = null;
    }

    public Long getId() {
        return id;
    }

    public ConcertDate getConcertDate() {
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
