package kr.hhplus.be.server.reservation.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import kr.hhplus.be.server.reservation.adapter.jpa.ReservationEntity;
import kr.hhplus.be.server.reservation.adapter.jpa.ReservationJpaRepository;
import kr.hhplus.be.server.reservation.domain.ReservationStatus;
import kr.hhplus.be.server.reservation.port.ClockProvider;
import kr.hhplus.be.server.reservation.port.SeatPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 만료된 좌석 임시 배정을 해제하는 스케줄러
 * 
 * 주기적으로 만료된 HOLD 상태의 예약을 찾아서:
 * 1. 예약 상태를 EXPIRED로 변경
 * 2. 해당 좌석의 HOLD 상태를 해제하여 AVAILABLE로 변경
 */
@Component
public class ReservationExpirationScheduler {
    private static final Logger log = LoggerFactory.getLogger(ReservationExpirationScheduler.class);

    private final ReservationJpaRepository reservationJpaRepository;
    private final SeatPort seatPort;
    private final ClockProvider clockProvider;

    public ReservationExpirationScheduler(
        ReservationJpaRepository reservationJpaRepository,
        SeatPort seatPort,
        ClockProvider clockProvider
    ) {
        this.reservationJpaRepository = reservationJpaRepository;
        this.seatPort = seatPort;
        this.clockProvider = clockProvider;
    }

    /**
     * 매 1분마다 실행되는 스케줄러
     * 만료된 HOLD 상태의 예약을 찾아서 해제 처리
     */
    @Scheduled(fixedRate = 60000) // 1분마다 실행
    @Transactional
    public void expireHeldReservations() {
        LocalDateTime now = clockProvider.now();
        List<ReservationEntity> expiredReservations = reservationJpaRepository.findExpiredHolds(
            ReservationStatus.HOLD,
            now
        );

        if (expiredReservations.isEmpty()) {
            return;
        }

        log.info("Found {} expired reservations to release", expiredReservations.size());

        for (ReservationEntity reservation : expiredReservations) {
            try {
                // 예약 상태를 EXPIRED로 변경
                reservation.updateStatus(ReservationStatus.EXPIRED, now);
                reservationJpaRepository.save(reservation);

                // 좌석의 HOLD 상태 해제
                var seat = seatPort.loadForUpdate(reservation.getSeatId());
                if (seat.isHeld() && seat.isHoldExpired(now)) {
                    seat.releaseHold();
                    seatPort.save(seat);
                    log.debug("Released seat {} from expired reservation {}", seat.getId(), reservation.getId());
                }
            } catch (Exception e) {
                log.error("Failed to expire reservation {}: {}", reservation.getId(), e.getMessage(), e);
            }
        }

        log.info("Completed expiration processing for {} reservations", expiredReservations.size());
    }
}
