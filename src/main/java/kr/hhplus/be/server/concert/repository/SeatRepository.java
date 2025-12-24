package kr.hhplus.be.server.concert.repository;

import java.util.List;
import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.concert.domain.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    long countByConcertDateId(Long concertDateId);

    List<Seat> findByConcertDateIdAndStatus(Long concertDateId, SeatStatus status);
}
