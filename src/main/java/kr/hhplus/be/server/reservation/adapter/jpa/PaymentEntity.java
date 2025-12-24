package kr.hhplus.be.server.reservation.adapter.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
public class PaymentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "amount", nullable = false)
    private long amount;

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;

    protected PaymentEntity() {
    }

    public PaymentEntity(Long reservationId, String userId, long amount, LocalDateTime paidAt) {
        this.reservationId = reservationId;
        this.userId = userId;
        this.amount = amount;
        this.paidAt = paidAt;
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
