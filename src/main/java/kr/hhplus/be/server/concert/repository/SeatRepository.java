package kr.hhplus.be.server.concert.repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.concert.domain.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    long countByConcertDateId(Long concertDateId);

    List<Seat> findByConcertDateIdAndStatus(Long concertDateId, SeatStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s join s.concertDate d where d.concertDate = :date and s.seatNumber = :seatNumber")
    Optional<Seat> findForUpdateByDateAndSeatNumber(
        @Param("date") LocalDate date,
        @Param("seatNumber") int seatNumber
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.id = :seatId")
    Optional<Seat> findForUpdateById(@Param("seatId") Long seatId);
}
