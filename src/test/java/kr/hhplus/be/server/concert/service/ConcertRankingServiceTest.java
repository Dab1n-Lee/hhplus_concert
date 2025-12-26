package kr.hhplus.be.server.concert.service;

import java.time.LocalDate;
import java.util.List;
import kr.hhplus.be.server.ServerApplicationTests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class ConcertRankingServiceTest extends ServerApplicationTests {

    @Autowired
    private ConcertRankingService concertRankingService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String RANKING_KEY = "concert:sold-out-ranking";

    @BeforeEach
    void setUp() {
        // 테스트 전 랭킹 데이터 초기화
        redisTemplate.delete(RANKING_KEY);
    }

    @Test
    @DisplayName("결제 완료 시 해당 날짜의 예약 완료 좌석 수가 증가한다")
    void incrementSoldOutCount() {
        // given
        LocalDate concertDate = LocalDate.of(2025, 1, 10);

        // when
        concertRankingService.incrementSoldOutCount(concertDate);
        Long count = concertRankingService.getSoldOutCount(concertDate);

        // then
        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("여러 번 결제가 완료되면 예약 완료 좌석 수가 누적된다")
    void incrementSoldOutCountMultiple() {
        // given
        LocalDate concertDate = LocalDate.of(2025, 1, 10);

        // when
        concertRankingService.incrementSoldOutCount(concertDate);
        concertRankingService.incrementSoldOutCount(concertDate);
        concertRankingService.incrementSoldOutCount(concertDate);
        Long count = concertRankingService.getSoldOutCount(concertDate);

        // then
        assertThat(count).isEqualTo(3L);
    }

    @Test
    @DisplayName("빠른 매진 랭킹을 조회할 수 있다")
    void getTopRanking() {
        // given
        LocalDate date1 = LocalDate.of(2025, 1, 10);
        LocalDate date2 = LocalDate.of(2025, 1, 11);
        LocalDate date3 = LocalDate.of(2025, 1, 12);

        // date1: 5개, date2: 10개, date3: 3개 예약 완료
        for (int i = 0; i < 5; i++) {
            concertRankingService.incrementSoldOutCount(date1);
        }
        for (int i = 0; i < 10; i++) {
            concertRankingService.incrementSoldOutCount(date2);
        }
        for (int i = 0; i < 3; i++) {
            concertRankingService.incrementSoldOutCount(date3);
        }

        // when
        List<ConcertRankingService.ConcertRanking> rankings = concertRankingService.getTopRanking(10);

        // then
        assertThat(rankings).hasSize(3);
        assertThat(rankings.get(0).getConcertDate()).isEqualTo(date2);
        assertThat(rankings.get(0).getSoldOutCount()).isEqualTo(10L);
        assertThat(rankings.get(1).getConcertDate()).isEqualTo(date1);
        assertThat(rankings.get(1).getSoldOutCount()).isEqualTo(5L);
        assertThat(rankings.get(2).getConcertDate()).isEqualTo(date3);
        assertThat(rankings.get(2).getSoldOutCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("랭킹이 없을 때 빈 리스트를 반환한다")
    void getTopRankingWhenEmpty() {
        // when
        List<ConcertRankingService.ConcertRanking> rankings = concertRankingService.getTopRanking(10);

        // then
        assertThat(rankings).isEmpty();
    }

    @Test
    @DisplayName("limit 파라미터만큼만 랭킹을 조회한다")
    void getTopRankingWithLimit() {
        // given
        LocalDate date1 = LocalDate.of(2025, 1, 10);
        LocalDate date2 = LocalDate.of(2025, 1, 11);
        LocalDate date3 = LocalDate.of(2025, 1, 12);

        concertRankingService.incrementSoldOutCount(date1);
        concertRankingService.incrementSoldOutCount(date2);
        concertRankingService.incrementSoldOutCount(date3);

        // when
        List<ConcertRankingService.ConcertRanking> rankings = concertRankingService.getTopRanking(2);

        // then
        assertThat(rankings).hasSize(2);
    }

    @Test
    @DisplayName("특정 날짜의 순위를 조회할 수 있다")
    void getRank() {
        // given
        LocalDate date1 = LocalDate.of(2025, 1, 10);
        LocalDate date2 = LocalDate.of(2025, 1, 11);
        LocalDate date3 = LocalDate.of(2025, 1, 12);

        concertRankingService.incrementSoldOutCount(date1); // 1개
        concertRankingService.incrementSoldOutCount(date2);
        concertRankingService.incrementSoldOutCount(date2); // 2개
        concertRankingService.incrementSoldOutCount(date3);
        concertRankingService.incrementSoldOutCount(date3);
        concertRankingService.incrementSoldOutCount(date3); // 3개

        // when
        Long rank1 = concertRankingService.getRank(date1);
        Long rank2 = concertRankingService.getRank(date2);
        Long rank3 = concertRankingService.getRank(date3);

        // then
        // date3: 3개 (1등), date2: 2개 (2등), date1: 1개 (3등)
        assertThat(rank1).isEqualTo(3L);
        assertThat(rank2).isEqualTo(2L);
        assertThat(rank3).isEqualTo(1L);
    }

    @Test
    @DisplayName("랭킹에 없는 날짜의 순위는 null을 반환한다")
    void getRankWhenNotExists() {
        // given
        LocalDate concertDate = LocalDate.of(2025, 1, 10);

        // when
        Long rank = concertRankingService.getRank(concertDate);

        // then
        assertThat(rank).isNull();
    }
}
