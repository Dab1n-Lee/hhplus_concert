package kr.hhplus.be.server.point.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "user_point",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id"})
)
public class UserPoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "balance", nullable = false)
    private long balance;

    protected UserPoint() {
    }

    public UserPoint(String userId, long balance) {
        this.userId = userId;
        this.balance = balance;
    }

    public void charge(long amount) {
        this.balance += amount;
    }

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public long getBalance() {
        return balance;
    }
}
