package kr.hhplus.be.server.concert.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import kr.hhplus.be.server.concert.domain.ConcertDate;
import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.concert.domain.SeatStatus;
import kr.hhplus.be.server.concert.repository.ConcertDateRepository;
import kr.hhplus.be.server.concert.repository.SeatRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConcertQueryService {
    private static final int MAX_SEAT_NUMBER = 50;
    private static final String CACHE_AVAILABLE_DATES = "availableDates";
    private static final String CACHE_AVAILABLE_SEATS = "availableSeats";

    private final ConcertDateRepository concertDateRepository;
    private final SeatRepository seatRepository;

    public ConcertQueryService(ConcertDateRepository concertDateRepository, SeatRepository seatRepository) {
        this.concertDateRepository = concertDateRepository;
        this.seatRepository = seatRepository;
    }

    @Cacheable(value = CACHE_AVAILABLE_DATES, key = "'all'")
    @Transactional(readOnly = true)
    public List<LocalDate> getAvailableDates() {
        return concertDateRepository.findAll().stream()
            .map(ConcertDate::getConcertDate)
            .sorted()
            .toList();
    }

    @Cacheable(value = CACHE_AVAILABLE_SEATS, key = "#date.toString()")
    @Transactional(readOnly = true)
    public List<Seat> getAvailableSeats(LocalDate date) {
        ConcertDate concertDate = concertDateRepository.findByConcertDate(date)
            .orElseGet(() -> concertDateRepository.save(new ConcertDate(date)));

        if (seatRepository.countByConcertDateId(concertDate.getId()) == 0) {
            List<Seat> seats = IntStream.rangeClosed(1, MAX_SEAT_NUMBER)
                .mapToObj(seatNumber -> Seat.available(concertDate, seatNumber))
                .toList();
            seatRepository.saveAll(seats);
        }

        return seatRepository.findByConcertDateIdAndStatus(concertDate.getId(), SeatStatus.AVAILABLE);
    }

    /**
     * Evict cache when seat status changes (e.g., after reservation or payment)
     */
    @CacheEvict(value = CACHE_AVAILABLE_SEATS, key = "#date.toString()")
    public void evictAvailableSeatsCache(LocalDate date) {
        // Cache eviction is handled by annotation
    }

    /**
     * Evict all caches periodically to ensure data freshness
     * Runs every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    @CacheEvict(value = {CACHE_AVAILABLE_DATES, CACHE_AVAILABLE_SEATS}, allEntries = true)
    public void evictAllCaches() {
        // Cache eviction is handled by annotation
    }
}
