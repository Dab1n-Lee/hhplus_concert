package kr.hhplus.be.server.reservation.adapter;

import java.time.LocalDateTime;
import kr.hhplus.be.server.reservation.adapter.jpa.ReservationEntity;
import kr.hhplus.be.server.reservation.adapter.jpa.ReservationJpaRepository;
import kr.hhplus.be.server.reservation.domain.Reservation;
import kr.hhplus.be.server.reservation.domain.ReservationStatus;
import kr.hhplus.be.server.reservation.port.SeatReservationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ReservationJpaAdapter implements SeatReservationRepository {
    private final ReservationJpaRepository reservationJpaRepository;

    public ReservationJpaAdapter(ReservationJpaRepository reservationJpaRepository) {
        this.reservationJpaRepository = reservationJpaRepository;
    }

    @Override
    @Transactional
    public Reservation save(Reservation reservation) {
        ReservationEntity entity;
        if (reservation.getId() == null) {
            entity = new ReservationEntity(
                reservation.getSeatId(),
                reservation.getUserId(),
                reservation.getStatus(),
                reservation.getExpiresAt(),
                reservation.getCreatedAt()
            );
        } else {
            entity = reservationJpaRepository.findById(reservation.getId())
                .orElseThrow(() -> new IllegalStateException("Reservation not found."));
            entity.updateStatus(reservation.getStatus(), reservation.getExpiresAt());
        }

        ReservationEntity saved = reservationJpaRepository.save(entity);
        reservation.setId(saved.getId());
        return reservation;
    }

    @Override
    @Transactional
    public void expireActiveBySeatId(Long seatId, LocalDateTime expiredAt) {
        reservationJpaRepository.findFirstBySeatIdAndStatus(seatId, ReservationStatus.HOLD)
            .ifPresent(entity -> entity.updateStatus(ReservationStatus.EXPIRED, expiredAt));
    }

    @Override
    @Transactional
    public Reservation loadForUpdate(Long reservationId) {
        ReservationEntity entity = reservationJpaRepository.findById(reservationId)
            .orElseThrow(() -> new IllegalStateException("Reservation not found."));
        return Reservation.rebuild(
            entity.getId(),
            entity.getSeatId(),
            entity.getUserId(),
            entity.getStatus(),
            entity.getExpiresAt(),
            entity.getCreatedAt()
        );
    }
}
