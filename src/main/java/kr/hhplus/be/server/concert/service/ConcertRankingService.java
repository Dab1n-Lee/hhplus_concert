package kr.hhplus.be.server.concert.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ConcertRankingService {
    private static final String RANKING_KEY = "concert:sold-out-ranking";
    private static final int DEFAULT_TTL_SECONDS = 86400 * 30; // 30 days

    private final RedisTemplate<String, String> redisTemplate;

    public ConcertRankingService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 결제 완료 시 해당 날짜의 예약 완료 좌석 수를 1 증가시킵니다.
     * Redis Sorted Set의 ZINCRBY를 사용하여 원자적으로 처리됩니다.
     */
    public void incrementSoldOutCount(LocalDate concertDate) {
        String dateKey = concertDate.toString();
        redisTemplate.opsForZSet().incrementScore(RANKING_KEY, dateKey, 1);
        redisTemplate.expire(RANKING_KEY, java.time.Duration.ofSeconds(DEFAULT_TTL_SECONDS));
    }

    /**
     * 빠른 매진 랭킹을 조회합니다. (높은 점수 순 = 많은 예약 완료 수)
     * @param limit 조회할 상위 랭킹 개수
     * @return 콘서트 날짜와 예약 완료 좌석 수를 담은 랭킹 리스트
     */
    public List<ConcertRanking> getTopRanking(int limit) {
        Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>> rankingSet =
            redisTemplate.opsForZSet().reverseRangeWithScores(RANKING_KEY, 0, limit - 1);

        if (rankingSet == null) {
            return List.of();
        }

        return rankingSet.stream()
            .map(tuple -> new ConcertRanking(
                LocalDate.parse(tuple.getValue()),
                tuple.getScore() != null ? tuple.getScore().longValue() : 0L
            ))
            .collect(Collectors.toList());
    }

    /**
     * 특정 콘서트 날짜의 예약 완료 좌석 수를 조회합니다.
     */
    public Long getSoldOutCount(LocalDate concertDate) {
        String dateKey = concertDate.toString();
        Double score = redisTemplate.opsForZSet().score(RANKING_KEY, dateKey);
        return score != null ? score.longValue() : 0L;
    }

    /**
     * 특정 콘서트 날짜의 랭킹 순위를 조회합니다. (1등이 0번째)
     */
    public Long getRank(LocalDate concertDate) {
        String dateKey = concertDate.toString();
        Long rank = redisTemplate.opsForZSet().reverseRank(RANKING_KEY, dateKey);
        return rank != null ? rank + 1 : null; // 0-based to 1-based
    }

    public static class ConcertRanking {
        private final LocalDate concertDate;
        private final Long soldOutCount;

        public ConcertRanking(LocalDate concertDate, Long soldOutCount) {
            this.concertDate = concertDate;
            this.soldOutCount = soldOutCount;
        }

        public LocalDate getConcertDate() {
            return concertDate;
        }

        public Long getSoldOutCount() {
            return soldOutCount;
        }
    }
}
