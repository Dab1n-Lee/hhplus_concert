package kr.hhplus.be.server.queue.adapter.jpa;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

public interface ReservationTokenJpaRepository extends JpaRepository<ReservationTokenEntity, Long> {
    Optional<ReservationTokenEntity> findFirstByUserIdOrderByCreatedAtDesc(String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ReservationTokenEntity> findByToken(String token);
}
