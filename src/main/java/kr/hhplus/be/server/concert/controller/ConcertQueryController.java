package kr.hhplus.be.server.concert.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import kr.hhplus.be.server.concert.dto.AvailableSeatResponse;
import kr.hhplus.be.server.concert.dto.ConcertRankingResponse;
import kr.hhplus.be.server.concert.service.ConcertQueryService;
import kr.hhplus.be.server.concert.service.ConcertRankingService;
import kr.hhplus.be.server.queue.application.ReservationTokenService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/concerts")
public class ConcertQueryController {
    private final ConcertQueryService concertQueryService;
    private final ReservationTokenService reservationTokenService;
    private final ConcertRankingService concertRankingService;

    public ConcertQueryController(
        ConcertQueryService concertQueryService,
        ReservationTokenService reservationTokenService,
        ConcertRankingService concertRankingService
    ) {
        this.concertQueryService = concertQueryService;
        this.reservationTokenService = reservationTokenService;
        this.concertRankingService = concertRankingService;
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

    /**
     * 빠른 매진 랭킹 조회 API
     * 결제 완료가 많이 발생한 콘서트 날짜를 랭킹으로 반환합니다.
     */
    @GetMapping("/ranking")
    public List<ConcertRankingResponse> getSoldOutRanking(
        @RequestParam(defaultValue = "10") int limit
    ) {
        List<ConcertRankingService.ConcertRanking> rankings = concertRankingService.getTopRanking(limit);
        return IntStream.range(0, rankings.size())
            .mapToObj(i -> {
                ConcertRankingService.ConcertRanking ranking = rankings.get(i);
                return new ConcertRankingResponse(
                    ranking.getConcertDate(),
                    ranking.getSoldOutCount(),
                    (long) (i + 1) // 1-based rank
                );
            })
            .toList();
    }
}
