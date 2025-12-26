package kr.hhplus.be.server.reservation.application;

import java.time.LocalDateTime;
import kr.hhplus.be.server.concert.service.ConcertQueryService;
import kr.hhplus.be.server.concert.service.ConcertRankingService;
import kr.hhplus.be.server.lock.adapter.redis.SpinDistributedLock;
import kr.hhplus.be.server.reservation.application.event.ReservationCompletedEvent;
import kr.hhplus.be.server.reservation.domain.Payment;
import kr.hhplus.be.server.reservation.domain.ReservationStatus;
import kr.hhplus.be.server.reservation.port.ClockProvider;
import kr.hhplus.be.server.reservation.port.NotificationPort;
import kr.hhplus.be.server.reservation.port.PaymentRepository;
import kr.hhplus.be.server.reservation.port.SeatReservationRepository;
import kr.hhplus.be.server.reservation.port.UserBalanceRepository;
import kr.hhplus.be.server.reservation.port.SeatPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayReservationUseCase {
    private static final long LOCK_WAIT_TIME_MS = 1000; // 1 second
    private static final long LOCK_LEASE_TIME_MS = 5000; // 5 seconds

    private final SeatReservationRepository reservationPort;
    private final SeatPort seatPort;
    private final UserBalanceRepository pointPort;
    private final PaymentRepository paymentPort;
    private final NotificationPort notificationPort;
    private final ClockProvider clockProvider;
    private final SpinDistributedLock distributedLock;
    private final ConcertQueryService concertQueryService;
    private final ConcertRankingService concertRankingService;
    private final ApplicationEventPublisher eventPublisher;

    public PayReservationUseCase(
        SeatReservationRepository reservationPort,
        SeatPort seatPort,
        UserBalanceRepository pointPort,
        PaymentRepository paymentPort,
        NotificationPort notificationPort,
        ClockProvider clockProvider,
        SpinDistributedLock distributedLock,
        ConcertQueryService concertQueryService,
        ConcertRankingService concertRankingService,
        ApplicationEventPublisher eventPublisher
    ) {
        this.reservationPort = reservationPort;
        this.seatPort = seatPort;
        this.pointPort = pointPort;
        this.paymentPort = paymentPort;
        this.notificationPort = notificationPort;
        this.clockProvider = clockProvider;
        this.distributedLock = distributedLock;
        this.concertQueryService = concertQueryService;
        this.concertRankingService = concertRankingService;
        this.eventPublisher = eventPublisher;
    }

    public Payment pay(PayReservationCommand command) {
        // Lock key: reservation-specific lock to prevent concurrent payment of the same reservation
        String lockKey = "reservation:pay:" + command.getReservationId();
        
        return distributedLock.executeWithLock(
            lockKey,
            LOCK_WAIT_TIME_MS,
            LOCK_LEASE_TIME_MS,
            () -> payInternal(command)
        );
    }

    @Transactional
    private Payment payInternal(PayReservationCommand command) {
        LocalDateTime now = clockProvider.now();
        var reservation = reservationPort.loadForUpdate(command.getReservationId());

        if (!reservation.getUserId().equals(command.getUserId())) {
            throw new IllegalArgumentException("Reservation does not belong to user.");
        }

        if (reservation.getStatus() != ReservationStatus.HOLD) {
            throw new IllegalStateException("Reservation is not in hold status.");
        }

        if (reservation.isExpired(now)) {
            reservation.expire(now);
            reservationPort.save(reservation);
            var seat = seatPort.loadForUpdate(reservation.getSeatId());
            seat.releaseHold();
            seatPort.save(seat);
            throw new IllegalStateException("Reservation expired.");
        }

        var seat = seatPort.loadForUpdate(reservation.getSeatId());
        if (!seat.isHeldBy(command.getUserId())) {
            throw new IllegalStateException("Seat is not held by user.");
        }

        // pointPort.use() 내부에서 조건부 UPDATE를 사용하여 원자적으로 잔액 확인 및 차감
        // getBalance() 호출을 제거하여 동시성 문제 해결
        pointPort.use(command.getUserId(), command.getAmount());
        seat.reserve(command.getUserId());
        seatPort.save(seat);

        reservation.confirm(now);
        reservationPort.save(reservation);

        Payment payment = paymentPort.save(
            Payment.create(reservation.getId(), command.getUserId(), command.getAmount(), now)
        );
        notificationPort.sendReservationConfirmed(payment.getReservationId(), command.getUserId(), seat.getSeatNumber());

        // Evict cache for the concert date when payment is completed
        concertQueryService.evictAvailableSeatsCache(seat.getConcertDate());

        // Update ranking: increment sold-out count for the concert date
        concertRankingService.incrementSoldOutCount(seat.getConcertDate());

        // Publish reservation completed event
        ReservationCompletedEvent event = new ReservationCompletedEvent(
            reservation.getId(),
            command.getUserId(),
            payment.getId(),
            command.getAmount(),
            seat.getConcertDate(),
            seat.getSeatNumber()
        );
        eventPublisher.publishEvent(event);

        return payment;
    }
}
