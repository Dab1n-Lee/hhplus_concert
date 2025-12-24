package kr.hhplus.be.server.concert.repository;

import java.time.LocalDate;
import java.util.Optional;
import kr.hhplus.be.server.concert.domain.ConcertDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConcertDateRepository extends JpaRepository<ConcertDate, Long> {
    Optional<ConcertDate> findByConcertDate(LocalDate concertDate);
}
