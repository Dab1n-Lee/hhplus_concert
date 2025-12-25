package kr.hhplus.be.server.reservation.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import kr.hhplus.be.server.TestcontainersConfiguration;
import kr.hhplus.be.server.concert.domain.ConcertDate;
import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.concert.repository.ConcertDateRepository;
import kr.hhplus.be.server.concert.repository.SeatRepository;
import kr.hhplus.be.server.point.domain.UserPoint;
import kr.hhplus.be.server.point.repository.UserPointRepository;
import kr.hhplus.be.server.queue.adapter.jpa.ReservationTokenJpaRepository;
import kr.hhplus.be.server.reservation.adapter.jpa.PaymentJpaRepository;
import kr.hhplus.be.server.reservation.adapter.jpa.ReservationJpaRepository;
import kr.hhplus.be.server.reservation.application.PayReservationCommand;
import kr.hhplus.be.server.reservation.application.PayReservationUseCase;
import kr.hhplus.be.server.reservation.application.ReserveSeatCommand;
import kr.hhplus.be.server.reservation.application.ReserveSeatUseCase;
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
class BalanceDeductionConcurrencyTest {
    @Autowired
    private ReserveSeatUseCase reserveSeatUseCase;

    @Autowired
    private PayReservationUseCase payReservationUseCase;

    @Autowired
    private ReservationJpaRepository reservationJpaRepository;

    @Autowired
    private PaymentJpaRepository paymentJpaRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ConcertDateRepository concertDateRepository;

    @Autowired
    private UserPointRepository userPointRepository;

    @Autowired
    private ReservationTokenJpaRepository reservationTokenJpaRepository;

    @Autowired
    private TestClockProvider testClockProvider;

    private static final String TEST_USER_ID = "user-balance-test";
    private static final long INITIAL_BALANCE = 1000L;
    private static final long PAYMENT_AMOUNT = 100L;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 포인트 생성
        userPointRepository.save(new UserPoint(TEST_USER_ID, INITIAL_BALANCE));
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
    void preventsNegativeBalanceWhenMultiplePaymentsConcurrently() throws Exception {
        LocalDate concertDate = LocalDate.of(2025, 1, 1);
        ConcertDate dateEntity = concertDateRepository.save(new ConcertDate(concertDate));
        Seat seat1 = seatRepository.save(Seat.available(dateEntity, 1));
        Seat seat2 = seatRepository.save(Seat.available(dateEntity, 2));
        Seat seat3 = seatRepository.save(Seat.available(dateEntity, 3));
        Seat seat4 = seatRepository.save(Seat.available(dateEntity, 4));
        Seat seat5 = seatRepository.save(Seat.available(dateEntity, 5));

        testClockProvider.setNow(LocalDateTime.of(2025, 1, 1, 10, 0));

        // 5개의 좌석을 예약 (각각 100포인트)
        Long reservationId1 = reserveSeatUseCase.reserve(
            new ReserveSeatCommand(TEST_USER_ID, concertDate, 1)).getId();
        Long reservationId2 = reserveSeatUseCase.reserve(
            new ReserveSeatCommand(TEST_USER_ID, concertDate, 2)).getId();
        Long reservationId3 = reserveSeatUseCase.reserve(
            new ReserveSeatCommand(TEST_USER_ID, concertDate, 3)).getId();
        Long reservationId4 = reserveSeatUseCase.reserve(
            new ReserveSeatCommand(TEST_USER_ID, concertDate, 4)).getId();
        Long reservationId5 = reserveSeatUseCase.reserve(
            new ReserveSeatCommand(TEST_USER_ID, concertDate, 5)).getId();

        // 초기 잔액 1000, 각 결제 100 -> 최대 10개 결제 가능
        // 하지만 5개만 예약했으므로 모두 성공해야 함
        // 동시에 5개 결제 요청을 보내서 잔액이 음수가 되지 않는지 확인

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        List<Long> reservationIds = List.of(reservationId1, reservationId2, reservationId3, reservationId4, reservationId5);

        for (int i = 0; i < threadCount; i++) {
            Long reservationId = reservationIds.get(i);
            results.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    payReservationUseCase.pay(
                        new PayReservationCommand(reservationId, TEST_USER_ID, PAYMENT_AMOUNT));
                    return true;
                } catch (RuntimeException ex) {
                    return false;
                }
            }));
        }

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();

        int successCount = 0;
        for (Future<Boolean> result : results) {
            if (result.get(10, TimeUnit.SECONDS)) {
                successCount++;
            }
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // 모든 결제가 성공해야 함 (잔액 1000, 각 100씩 5개 = 500)
        assertThat(successCount).isEqualTo(5);
        
        // 최종 잔액 확인 (1000 - 500 = 500)
        UserPoint finalUserPoint = userPointRepository.findByUserId(TEST_USER_ID)
            .orElseThrow();
        assertThat(finalUserPoint.getBalance()).isEqualTo(INITIAL_BALANCE - (PAYMENT_AMOUNT * 5));
        assertThat(finalUserPoint.getBalance()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void preventsOverDeductionWhenBalanceIsInsufficient() throws Exception {
        LocalDate concertDate = LocalDate.of(2025, 1, 1);
        ConcertDate dateEntity = concertDateRepository.save(new ConcertDate(concertDate));
        Seat seat1 = seatRepository.save(Seat.available(dateEntity, 1));
        Seat seat2 = seatRepository.save(Seat.available(dateEntity, 2));
        Seat seat3 = seatRepository.save(Seat.available(dateEntity, 3));

        testClockProvider.setNow(LocalDateTime.of(2025, 1, 1, 10, 0));

        // 3개의 좌석을 예약
        Long reservationId1 = reserveSeatUseCase.reserve(
            new ReserveSeatCommand(TEST_USER_ID, concertDate, 1)).getId();
        Long reservationId2 = reserveSeatUseCase.reserve(
            new ReserveSeatCommand(TEST_USER_ID, concertDate, 2)).getId();
        Long reservationId3 = reserveSeatUseCase.reserve(
            new ReserveSeatCommand(TEST_USER_ID, concertDate, 3)).getId();

        // 초기 잔액 1000, 각 결제 500 -> 최대 2개만 결제 가능
        // 3개를 동시에 결제 요청하면 1개는 실패해야 함

        int threadCount = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        List<Long> reservationIds = List.of(reservationId1, reservationId2, reservationId3);
        long largeAmount = 500L; // 각 결제 500포인트

        for (int i = 0; i < threadCount; i++) {
            Long reservationId = reservationIds.get(i);
            results.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    payReservationUseCase.pay(
                        new PayReservationCommand(reservationId, TEST_USER_ID, largeAmount));
                    return true;
                } catch (RuntimeException ex) {
                    return false;
                }
            }));
        }

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();

        int successCount = 0;
        for (Future<Boolean> result : results) {
            if (result.get(10, TimeUnit.SECONDS)) {
                successCount++;
            }
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // 최대 2개만 성공해야 함 (잔액 1000, 각 500씩)
        assertThat(successCount).isLessThanOrEqualTo(2);
        
        // 최종 잔액이 음수가 아니어야 함
        UserPoint finalUserPoint = userPointRepository.findByUserId(TEST_USER_ID)
            .orElseThrow();
        assertThat(finalUserPoint.getBalance()).isGreaterThanOrEqualTo(0);
        assertThat(finalUserPoint.getBalance()).isLessThanOrEqualTo(INITIAL_BALANCE);
    }
}
