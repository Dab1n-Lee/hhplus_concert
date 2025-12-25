package kr.hhplus.be.server.reservation.adapter.jpa;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import kr.hhplus.be.server.reservation.domain.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ReservationJpaRepository extends JpaRepository<ReservationEntity, Long> {
    Optional<ReservationEntity> findFirstBySeatIdAndStatus(Long seatId, ReservationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ReservationEntity> findById(Long id);

    /**
     * 만료된 HOLD 상태의 예약 목록을 조회
     * @param now 현재 시간
     * @return 만료된 HOLD 상태의 예약 목록
     */
    @Query("select r from ReservationEntity r where r.status = :status and r.expiresAt < :now")
    List<ReservationEntity> findExpiredHolds(
        @Param("status") ReservationStatus status,
        @Param("now") LocalDateTime now
    );
}
