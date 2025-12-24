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
import kr.hhplus.be.server.point.repository.UserPointRepository;
import kr.hhplus.be.server.queue.adapter.jpa.ReservationTokenJpaRepository;
import kr.hhplus.be.server.reservation.adapter.jpa.PaymentJpaRepository;
import kr.hhplus.be.server.reservation.adapter.jpa.ReservationJpaRepository;
import kr.hhplus.be.server.reservation.application.ReserveSeatCommand;
import kr.hhplus.be.server.reservation.application.ReserveSeatUseCase;
import kr.hhplus.be.server.support.TestClockConfiguration;
import kr.hhplus.be.server.support.TestClockProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestClockConfiguration.class})
class ReservationConcurrencyIntegrationTest {
    @Autowired
    private ReserveSeatUseCase reserveSeatUseCase;

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
    void allowsOnlyOneReservationForSameSeatConcurrently() throws Exception {
        LocalDate concertDate = LocalDate.of(2025, 1, 1);
        ConcertDate dateEntity = concertDateRepository.save(new ConcertDate(concertDate));
        seatRepository.save(Seat.available(dateEntity, 10));

        testClockProvider.setNow(LocalDateTime.of(2025, 1, 1, 10, 0));

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            String userId = "user-" + i;
            results.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    reserveSeatUseCase.reserve(new ReserveSeatCommand(userId, concertDate, 10));
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

        assertThat(successCount).isEqualTo(1);
        assertThat(reservationJpaRepository.count()).isEqualTo(1);
    }
}
