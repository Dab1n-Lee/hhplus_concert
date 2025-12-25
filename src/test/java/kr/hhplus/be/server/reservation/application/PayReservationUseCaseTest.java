package kr.hhplus.be.server.reservation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @InjectMocks
    private PayReservationUseCase payReservationUseCase;

    @Test
    void paysReservationAndReservesSeat() {
        LocalDateTime now = LocalDateTime.of(2025, 1, 1, 12, 0);
        Reservation reservation = Reservation.hold(1L, "user-1", now.plusMinutes(5), now.minusMinutes(1));
        reservation.setId(101L);
        Seat seat = new Seat(1L, LocalDate.of(2025, 1, 1), 10, SeatStatus.HELD, "user-1", now.plusMinutes(5), null);

        when(clockProvider.now()).thenReturn(now);
        when(reservationPort.loadForUpdate(101L)).thenReturn(reservation);
        when(seatPort.loadForUpdate(1L)).thenReturn(seat);
        when(paymentPort.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(500L);
            return payment;
        });

        Payment payment = payReservationUseCase.pay(new PayReservationCommand(101L, "user-1", 50L));

        ArgumentCaptor<Seat> seatCaptor = ArgumentCaptor.forClass(Seat.class);
        verify(seatPort).save(seatCaptor.capture());
        assertThat(seatCaptor.getValue().getStatus()).isEqualTo(SeatStatus.RESERVED);
        assertThat(payment.getId()).isEqualTo(500L);
        verify(pointPort).use("user-1", 50L);
        verify(notificationPort).sendReservationConfirmed(101L, "user-1", 10);
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

        assertThatThrownBy(() -> payReservationUseCase.pay(new PayReservationCommand(101L, "user-1", 50L)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Insufficient points");
    }
}
