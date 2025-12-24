package kr.hhplus.be.server.reservation.adapter.jpa;

import java.util.Optional;
import kr.hhplus.be.server.reservation.domain.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

public interface ReservationJpaRepository extends JpaRepository<ReservationEntity, Long> {
    Optional<ReservationEntity> findFirstBySeatIdAndStatus(Long seatId, ReservationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ReservationEntity> findById(Long id);
}
