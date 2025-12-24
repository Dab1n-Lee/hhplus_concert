package kr.hhplus.be.server.concert.controller;

import java.time.LocalDate;
import java.util.List;
import kr.hhplus.be.server.concert.dto.AvailableSeatResponse;
import kr.hhplus.be.server.concert.service.ConcertQueryService;
import kr.hhplus.be.server.queue.application.ReservationTokenService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/concerts")
public class ConcertQueryController {
    private final ConcertQueryService concertQueryService;
    private final ReservationTokenService reservationTokenService;

    public ConcertQueryController(
        ConcertQueryService concertQueryService,
        ReservationTokenService reservationTokenService
    ) {
        this.concertQueryService = concertQueryService;
        this.reservationTokenService = reservationTokenService;
    }

    @GetMapping("/dates")
    public List<LocalDate> getAvailableDates(@RequestHeader("Queue-Token") String queueToken) {
        reservationTokenService.validateActive(queueToken);
        return concertQueryService.getAvailableDates();
    }

    @GetMapping("/dates/{date}/seats")
    public List<AvailableSeatResponse> getAvailableSeats(
        @RequestHeader("Queue-Token") String queueToken,
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        reservationTokenService.validateActive(queueToken);
        return concertQueryService.getAvailableSeats(date).stream()
            .map(seat -> new AvailableSeatResponse(seat.getSeatNumber()))
            .toList();
    }
}
