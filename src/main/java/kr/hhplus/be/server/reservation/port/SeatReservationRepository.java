package kr.hhplus.be.server.reservation.port;

import java.time.LocalDateTime;
import kr.hhplus.be.server.reservation.domain.Reservation;

public interface SeatReservationRepository {
    Reservation save(Reservation reservation);

    void expireActiveBySeatId(Long seatId, LocalDateTime expiredAt);

    Reservation loadForUpdate(Long reservationId);
}
