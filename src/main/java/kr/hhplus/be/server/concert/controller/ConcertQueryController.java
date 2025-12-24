package kr.hhplus.be.server.concert.controller;

import java.time.LocalDate;
import java.util.List;
import kr.hhplus.be.server.concert.dto.AvailableSeatResponse;
import kr.hhplus.be.server.concert.service.ConcertQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/concerts")
public class ConcertQueryController {
    private final ConcertQueryService concertQueryService;

    public ConcertQueryController(ConcertQueryService concertQueryService) {
        this.concertQueryService = concertQueryService;
    }

    @GetMapping("/dates")
    public List<LocalDate> getAvailableDates() {
        return concertQueryService.getAvailableDates();
    }

    @GetMapping("/dates/{date}/seats")
    public List<AvailableSeatResponse> getAvailableSeats(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return concertQueryService.getAvailableSeats(date).stream()
            .map(seat -> new AvailableSeatResponse(seat.getSeatNumber()))
            .toList();
    }
}
