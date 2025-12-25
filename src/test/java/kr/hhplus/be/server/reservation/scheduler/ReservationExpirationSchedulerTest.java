package kr.hhplus.be.server.reservation.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.hhplus.be.server.TestcontainersConfiguration;
import kr.hhplus.be.server.concert.domain.ConcertDate;
import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.concert.domain.SeatStatus;
import kr.hhplus.be.server.concert.repository.ConcertDateRepository;
import kr.hhplus.be.server.concert.repository.SeatRepository;
import kr.hhplus.be.server.point.repository.UserPointRepository;
import kr.hhplus.be.server.queue.adapter.jpa.ReservationTokenJpaRepository;
import kr.hhplus.be.server.reservation.adapter.jpa.PaymentJpaRepository;
import kr.hhplus.be.server.reservation.adapter.jpa.ReservationEntity;
import kr.hhplus.be.server.reservation.adapter.jpa.ReservationJpaRepository;
import kr.hhplus.be.server.reservation.domain.ReservationStatus;
import kr.hhplus.be.server.support.TestClockConfiguration;
import kr.hhplus.be.server.support.TestClockProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestClockConfiguration.class})
class ReservationExpirationSchedulerTest {
    @Autowired
    private ReservationExpirationScheduler scheduler;

    @Autowired
    private ReservationJpaRepository reservationJpaRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ConcertDateRepository concertDateRepository;

    @Autowired
    private PaymentJpaRepository paymentJpaRepository;

    @Autowired
    private UserPointRepository userPointRepository;

    @Autowired
    private ReservationTokenJpaRepository reservationTokenJpaRepository;

    @Autowired
    private TestClockProvider testClockProvider;

    @BeforeEach
    void setUp() {
        testClockProvider.setNow(LocalDateTime.of(2025, 1, 1, 10, 0));
    }

    @AfterEach
    void tearDown() {
        paymentJpaRepository.deleteAll();
        reservationJpaRepository.deleteAll();
        seatRepository.deleteAll();
        concertDateRepository.deleteAll();
        userPointRepository.deleteAll();
        reservationTokenJpaRepository.deleteAll();
    }

    @Test
    void expiresHeldReservationsAndReleasesSeats() {
        // Given: 만료된 HOLD 상태의 예약과 좌석 생성
        LocalDate concertDate = LocalDate.of(2025, 1, 1);
        ConcertDate dateEntity = concertDateRepository.save(new ConcertDate(concertDate));
        Seat seat1 = seatRepository.save(Seat.available(dateEntity, 1));
        Seat seat2 = seatRepository.save(Seat.available(dateEntity, 2));

        // 좌석을 HOLD 상태로 변경
        seat1.hold("user-1", testClockProvider.now().minusMinutes(1)); // 만료됨
        seat2.hold("user-2", testClockProvider.now().plusMinutes(5)); // 아직 유효
        seatRepository.save(seat1);
        seatRepository.save(seat2);

        // 만료된 예약 생성
        ReservationEntity expiredReservation = new ReservationEntity(
            seat1.getId(),
            "user-1",
            ReservationStatus.HOLD,
            testClockProvider.now().minusMinutes(1), // 만료됨
            testClockProvider.now().minusMinutes(6)
        );
        reservationJpaRepository.save(expiredReservation);

        // 아직 유효한 예약 생성
        ReservationEntity validReservation = new ReservationEntity(
            seat2.getId(),
            "user-2",
            ReservationStatus.HOLD,
            testClockProvider.now().plusMinutes(5), // 아직 유효
            testClockProvider.now().minusMinutes(1)
        );
        reservationJpaRepository.save(validReservation);

        // When: 스케줄러 실행
        scheduler.expireHeldReservations();

        // Then: 만료된 예약만 EXPIRED로 변경되고 좌석이 해제됨
        ReservationEntity expired = reservationJpaRepository.findById(expiredReservation.getId())
            .orElseThrow();
        assertThat(expired.getStatus()).isEqualTo(ReservationStatus.EXPIRED);

        ReservationEntity valid = reservationJpaRepository.findById(validReservation.getId())
            .orElseThrow();
        assertThat(valid.getStatus()).isEqualTo(ReservationStatus.HOLD); // 아직 유효

        // 만료된 좌석은 AVAILABLE로 변경됨
        Seat releasedSeat = seatRepository.findById(seat1.getId()).orElseThrow();
        assertThat(releasedSeat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(releasedSeat.getHoldUserId()).isNull();
        assertThat(releasedSeat.getHoldExpiresAt()).isNull();

        // 유효한 좌석은 여전히 HELD 상태
        Seat heldSeat = seatRepository.findById(seat2.getId()).orElseThrow();
        assertThat(heldSeat.getStatus()).isEqualTo(SeatStatus.HELD);
    }

    @Test
    void handlesMultipleExpiredReservations() {
        // Given: 여러 개의 만료된 예약 생성
        LocalDate concertDate = LocalDate.of(2025, 1, 1);
        ConcertDate dateEntity = concertDateRepository.save(new ConcertDate(concertDate));

        int expiredCount = 5;
        for (int i = 1; i <= expiredCount; i++) {
            Seat seat = seatRepository.save(Seat.available(dateEntity, i));
            seat.hold("user-" + i, testClockProvider.now().minusMinutes(1));
            seatRepository.save(seat);

            ReservationEntity reservation = new ReservationEntity(
                seat.getId(),
                "user-" + i,
                ReservationStatus.HOLD,
                testClockProvider.now().minusMinutes(1),
                testClockProvider.now().minusMinutes(6)
            );
            reservationJpaRepository.save(reservation);
        }

        // When: 스케줄러 실행
        scheduler.expireHeldReservations();

        // Then: 모든 만료된 예약이 EXPIRED로 변경되고 좌석이 해제됨
        List<ReservationEntity> expiredReservations = reservationJpaRepository.findAll()
            .stream()
            .filter(r -> r.getStatus() == ReservationStatus.EXPIRED)
            .toList();
        assertThat(expiredReservations).hasSize(expiredCount);

        for (int i = 1; i <= expiredCount; i++) {
            Seat seat = seatRepository.findForUpdateByDateAndSeatNumber(concertDate, i)
                .orElseThrow();
            assertThat(seat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        }
    }

    @Test
    void doesNothingWhenNoExpiredReservations() {
        // Given: 만료되지 않은 예약만 존재
        LocalDate concertDate = LocalDate.of(2025, 1, 1);
        ConcertDate dateEntity = concertDateRepository.save(new ConcertDate(concertDate));
        Seat seat = seatRepository.save(Seat.available(dateEntity, 1));
        seat.hold("user-1", testClockProvider.now().plusMinutes(5));
        seatRepository.save(seat);

        ReservationEntity reservation = new ReservationEntity(
            seat.getId(),
            "user-1",
            ReservationStatus.HOLD,
            testClockProvider.now().plusMinutes(5),
            testClockProvider.now()
        );
        reservationJpaRepository.save(reservation);

        // When: 스케줄러 실행
        scheduler.expireHeldReservations();

        // Then: 예약 상태와 좌석 상태가 변경되지 않음
        ReservationEntity saved = reservationJpaRepository.findById(reservation.getId())
            .orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ReservationStatus.HOLD);

        Seat savedSeat = seatRepository.findById(seat.getId()).orElseThrow();
        assertThat(savedSeat.getStatus()).isEqualTo(SeatStatus.HELD);
    }
}
