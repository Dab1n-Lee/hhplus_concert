package kr.hhplus.be.server.concert.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import kr.hhplus.be.server.concert.domain.ConcertDate;
import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.concert.domain.SeatStatus;
import kr.hhplus.be.server.concert.repository.ConcertDateRepository;
import kr.hhplus.be.server.concert.repository.SeatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ConcertQueryServiceTest {
    @Mock
    private ConcertDateRepository concertDateRepository;

    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private ConcertQueryService concertQueryService;

    @Test
    void returnsSortedDates() {
        ConcertDate first = new ConcertDate(LocalDate.of(2025, 1, 1));
        ConcertDate second = new ConcertDate(LocalDate.of(2024, 12, 31));
        when(concertDateRepository.findAll()).thenReturn(List.of(first, second));

        List<LocalDate> dates = concertQueryService.getAvailableDates();

        assertThat(dates).containsExactly(LocalDate.of(2024, 12, 31), LocalDate.of(2025, 1, 1));
    }

    @Test
    void createsSeatsWhenNoneExist() {
        LocalDate date = LocalDate.of(2025, 1, 1);
        ConcertDate concertDate = new ConcertDate(date);
        ReflectionTestUtils.setField(concertDate, "id", 10L);

        when(concertDateRepository.findByConcertDate(date)).thenReturn(Optional.of(concertDate));
        when(seatRepository.countByConcertDateId(10L)).thenReturn(0L);
        when(seatRepository.findByConcertDateIdAndStatus(10L, SeatStatus.AVAILABLE))
            .thenReturn(List.of(Seat.available(concertDate, 1)));

        concertQueryService.getAvailableSeats(date);

        ArgumentCaptor<List<Seat>> captor = ArgumentCaptor.forClass(List.class);
        verify(seatRepository).saveAll(captor.capture());

        List<Seat> createdSeats = captor.getValue();
        assertThat(createdSeats).hasSize(50);
        assertThat(createdSeats.get(0).getSeatNumber()).isEqualTo(1);
        assertThat(createdSeats.get(createdSeats.size() - 1).getSeatNumber()).isEqualTo(50);
        verify(seatRepository).findByConcertDateIdAndStatus(10L, SeatStatus.AVAILABLE);
    }
}
