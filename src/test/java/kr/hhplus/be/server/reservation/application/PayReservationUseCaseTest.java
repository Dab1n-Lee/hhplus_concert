package kr.hhplus.be.server.reservation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import kr.hhplus.be.server.concert.service.ConcertQueryService;
import kr.hhplus.be.server.concert.service.ConcertRankingService;
import kr.hhplus.be.server.lock.adapter.redis.SpinDistributedLock;
import kr.hhplus.be.server.reservation.application.event.ReservationCompletedEvent;
import kr.hhplus.be.server.reservation.domain.Payment;
import kr.hhplus.be.server.reservation.domain.Reservation;
import kr.hhplus.be.server.reservation.domain.Seat;
import kr.hhplus.be.server.reservation.domain.SeatStatus;
import kr.hhplus.be.server.reservation.port.ClockProvider;
import kr.hhplus.be.server.reservation.port.NotificationPort;
import kr.hhplus.be.server.reservation.port.PaymentRepository;
import kr.hhplus.be.server.reservation.port.SeatReservationRepository;
import kr.hhplus.be.server.reservation.port.UserBalanceRepository;
import kr.hhplus.be.server.reservation.port.SeatPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PayReservationUseCaseTest {
    @Mock
    private SeatReservationRepository reservationPort;

    @Mock
    private SeatPort seatPort;

    @Mock
    private UserBalanceRepository pointPort;

    @Mock
    private PaymentRepository paymentPort;

    @Mock
    private NotificationPort notificationPort;

    @Mock
    private ClockProvider clockProvider;

    @Mock
    private SpinDistributedLock distributedLock;

    @Mock
    private ConcertQueryService concertQueryService;

    @Mock
    private ConcertRankingService concertRankingService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PayReservationUseCase payReservationUseCase;

    @Test
    void paysReservationAndReservesSeat() {
        LocalDateTime now = LocalDateTime.of(2025, 1, 1, 12, 0);
        LocalDate concertDate = LocalDate.of(2025, 1, 1);
        Reservation reservation = Reservation.hold(1L, "user-1", now.plusMinutes(5), now.minusMinutes(1));
        reservation.setId(101L);
        Seat seat = new Seat(1L, concertDate, 10, SeatStatus.HELD, "user-1", now.plusMinutes(5), null);

        when(clockProvider.now()).thenReturn(now);
        when(reservationPort.loadForUpdate(101L)).thenReturn(reservation);
        when(seatPort.loadForUpdate(1L)).thenReturn(seat);
        when(paymentPort.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(500L);
            return payment;
        });
        doAnswer(invocation -> invocation.getArgument(3, java.util.function.Supplier.class).get())
            .when(distributedLock).executeWithLock(any(String.class), any(Long.class), any(Long.class), any(java.util.function.Supplier.class));

        Payment payment = payReservationUseCase.pay(new PayReservationCommand(101L, "user-1", 50L));

        ArgumentCaptor<Seat> seatCaptor = ArgumentCaptor.forClass(Seat.class);
        verify(seatPort).save(seatCaptor.capture());
        assertThat(seatCaptor.getValue().getStatus()).isEqualTo(SeatStatus.RESERVED);
        assertThat(payment.getId()).isEqualTo(500L);
        verify(pointPort).use("user-1", 50L);
        verify(notificationPort).sendReservationConfirmed(101L, "user-1", 10);
    }

    @Test
    void publishesReservationCompletedEvent() {
        LocalDateTime now = LocalDateTime.of(2025, 1, 1, 12, 0);
        LocalDate concertDate = LocalDate.of(2025, 1, 1);
        Reservation reservation = Reservation.hold(1L, "user-1", now.plusMinutes(5), now.minusMinutes(1));
        reservation.setId(101L);
        Seat seat = new Seat(1L, concertDate, 10, SeatStatus.HELD, "user-1", now.plusMinutes(5), null);

        when(clockProvider.now()).thenReturn(now);
        when(reservationPort.loadForUpdate(101L)).thenReturn(reservation);
        when(seatPort.loadForUpdate(1L)).thenReturn(seat);
        when(paymentPort.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(500L);
            return payment;
        });
        doAnswer(invocation -> invocation.getArgument(3, java.util.function.Supplier.class).get())
            .when(distributedLock).executeWithLock(any(String.class), any(Long.class), any(Long.class), any(java.util.function.Supplier.class));

        payReservationUseCase.pay(new PayReservationCommand(101L, "user-1", 50L));

        ArgumentCaptor<ReservationCompletedEvent> eventCaptor = ArgumentCaptor.forClass(ReservationCompletedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        ReservationCompletedEvent event = eventCaptor.getValue();
        assertThat(event.getReservationId()).isEqualTo(101L);
        assertThat(event.getUserId()).isEqualTo("user-1");
        assertThat(event.getPaymentId()).isEqualTo(500L);
        assertThat(event.getAmount()).isEqualTo(50L);
        assertThat(event.getConcertDate()).isEqualTo(concertDate);
        assertThat(event.getSeatNumber()).isEqualTo(10);
    }

    @Test
    void rejectsInsufficientPoints() {
        LocalDateTime now = LocalDateTime.of(2025, 1, 1, 12, 0);
        Reservation reservation = Reservation.hold(1L, "user-1", now.plusMinutes(5), now.minusMinutes(1));
        reservation.setId(101L);
        Seat seat = new Seat(1L, LocalDate.of(2025, 1, 1), 10, SeatStatus.HELD, "user-1", now.plusMinutes(5), null);

        when(clockProvider.now()).thenReturn(now);
        when(reservationPort.loadForUpdate(101L)).thenReturn(reservation);
        when(seatPort.loadForUpdate(1L)).thenReturn(seat);
        org.mockito.Mockito.doThrow(new IllegalStateException("Insufficient points."))
            .when(pointPort).use("user-1", 50L);
        doAnswer(invocation -> invocation.getArgument(3, java.util.function.Supplier.class).get())
            .when(distributedLock).executeWithLock(any(String.class), any(Long.class), any(Long.class), any(java.util.function.Supplier.class));

        assertThatThrownBy(() -> payReservationUseCase.pay(new PayReservationCommand(101L, "user-1", 50L)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Insufficient points");
    }
}
