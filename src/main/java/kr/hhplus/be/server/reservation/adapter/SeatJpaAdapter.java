package kr.hhplus.be.server.reservation.adapter;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import kr.hhplus.be.server.concert.domain.ConcertDate;
import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.concert.repository.ConcertDateRepository;
import kr.hhplus.be.server.concert.repository.SeatRepository;
import kr.hhplus.be.server.reservation.port.SeatPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SeatJpaAdapter implements SeatPort {
    private static final int MAX_SEAT_NUMBER = 50;

    private final ConcertDateRepository concertDateRepository;
    private final SeatRepository seatRepository;

    public SeatJpaAdapter(ConcertDateRepository concertDateRepository, SeatRepository seatRepository) {
        this.concertDateRepository = concertDateRepository;
        this.seatRepository = seatRepository;
    }

    @Override
    @Transactional
    public kr.hhplus.be.server.reservation.domain.Seat loadForUpdate(LocalDate concertDate, int seatNumber) {
        ConcertDate dateEntity = concertDateRepository.findByConcertDate(concertDate)
            .orElseGet(() -> concertDateRepository.save(new ConcertDate(concertDate)));

        Seat seatEntity = seatRepository.findForUpdateByDateAndSeatNumber(concertDate, seatNumber)
            .orElseGet(() -> {
                if (seatRepository.countByConcertDateId(dateEntity.getId()) == 0) {
                    List<Seat> seats = IntStream.rangeClosed(1, MAX_SEAT_NUMBER)
                        .mapToObj(number -> Seat.available(dateEntity, number))
                        .toList();
                    seatRepository.saveAll(seats);
                }
                return seatRepository.findForUpdateByDateAndSeatNumber(concertDate, seatNumber)
                    .orElseThrow(() -> new IllegalStateException("Seat not found."));
            });

        return toDomain(seatEntity);
    }

    @Override
    @Transactional
    public kr.hhplus.be.server.reservation.domain.Seat loadForUpdate(Long seatId) {
        Seat seatEntity = seatRepository.findForUpdateById(seatId)
            .orElseThrow(() -> new IllegalStateException("Seat not found."));
        return toDomain(seatEntity);
    }

    @Override
    @Transactional
    public void save(kr.hhplus.be.server.reservation.domain.Seat seat) {
        Seat seatEntity = seatRepository.findById(seat.getId())
            .orElseThrow(() -> new IllegalStateException("Seat not found."));

        if (seat.getStatus() == kr.hhplus.be.server.reservation.domain.SeatStatus.AVAILABLE) {
            seatEntity.releaseHold();
        } else if (seat.getStatus() == kr.hhplus.be.server.reservation.domain.SeatStatus.HELD) {
            seatEntity.hold(seat.getHoldUserId(), seat.getHoldExpiresAt());
        } else {
            seatEntity.reserve(seat.getReservedUserId());
        }

        seatRepository.save(seatEntity);
    }

    private kr.hhplus.be.server.reservation.domain.Seat toDomain(Seat seatEntity) {
        return new kr.hhplus.be.server.reservation.domain.Seat(
            seatEntity.getId(),
            seatEntity.getConcertDate().getConcertDate(),
            seatEntity.getSeatNumber(),
            kr.hhplus.be.server.reservation.domain.SeatStatus.valueOf(seatEntity.getStatus().name()),
            seatEntity.getHoldUserId(),
            seatEntity.getHoldExpiresAt(),
            seatEntity.getReservedUserId()
        );
    }
}
