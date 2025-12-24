package kr.hhplus.be.server.reservation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import kr.hhplus.be.server.reservation.domain.Reservation;
import kr.hhplus.be.server.reservation.domain.Seat;
import kr.hhplus.be.server.reservation.domain.SeatStatus;
import kr.hhplus.be.server.reservation.port.ClockProvider;
import kr.hhplus.be.server.reservation.port.SeatReservationRepository;
import kr.hhplus.be.server.reservation.port.SeatPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReserveSeatUseCaseTest {
    @Mock
    private SeatPort seatPort;

    @Mock
    private SeatReservationRepository reservationPort;

    @Mock
    private ClockProvider clockProvider;

    @InjectMocks
    private ReserveSeatUseCase reserveSeatUseCase;

    @Test
    void reservesAvailableSeat() {
        LocalDate date = LocalDate.of(2025, 1, 1);
        LocalDateTime now = LocalDateTime.of(2025, 1, 1, 10, 0);
        Seat seat = new Seat(1L, date, 10, SeatStatus.AVAILABLE, null, null, null);

        when(clockProvider.now()).thenReturn(now);
        when(seatPort.loadForUpdate(date, 10)).thenReturn(seat);
        when(reservationPort.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation reservation = invocation.getArgument(0);
            reservation.setId(100L);
            return reservation;
        });

        Reservation reservation = reserveSeatUseCase.reserve(new ReserveSeatCommand("user-1", date, 10));

        ArgumentCaptor<Seat> captor = ArgumentCaptor.forClass(Seat.class);
        verify(seatPort).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(SeatStatus.HELD);
        assertThat(reservation.getId()).isEqualTo(100L);
        assertThat(reservation.getSeatId()).isEqualTo(1L);
    }

    @Test
    void rejectsUnavailableSeat() {
        LocalDate date = LocalDate.of(2025, 1, 1);
        LocalDateTime now = LocalDateTime.of(2025, 1, 1, 10, 0);
        Seat seat = new Seat(1L, date, 10, SeatStatus.HELD, "other-user", now.plusMinutes(1), null);

        when(clockProvider.now()).thenReturn(now);
        when(seatPort.loadForUpdate(date, 10)).thenReturn(seat);

        assertThatThrownBy(() -> reserveSeatUseCase.reserve(new ReserveSeatCommand("user-1", date, 10)))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsInvalidSeatNumber() {
        assertThatThrownBy(() -> reserveSeatUseCase.reserve(
            new ReserveSeatCommand("user-1", LocalDate.of(2025, 1, 1), 51)
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
