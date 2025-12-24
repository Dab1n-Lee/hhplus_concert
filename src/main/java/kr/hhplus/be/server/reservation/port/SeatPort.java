package kr.hhplus.be.server.reservation.port;

import java.time.LocalDate;
import kr.hhplus.be.server.reservation.domain.Seat;

public interface SeatPort {
    Seat loadForUpdate(LocalDate concertDate, int seatNumber);

    Seat loadForUpdate(Long seatId);

    void save(Seat seat);
}
