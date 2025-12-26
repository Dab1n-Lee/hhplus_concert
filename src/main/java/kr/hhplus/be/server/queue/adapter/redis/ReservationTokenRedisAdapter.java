package kr.hhplus.be.server.queue.adapter.redis;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import kr.hhplus.be.server.queue.domain.ReservationToken;
import kr.hhplus.be.server.queue.domain.ReservationTokenStatus;
import kr.hhplus.be.server.queue.port.ReservationTokenRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Primary
@Component
public class ReservationTokenRedisAdapter implements ReservationTokenRepository {
    private static final String WAITING_QUEUE_KEY = "queue:waiting";
    private static final String ACTIVE_TOKENS_KEY = "queue:active";
    private static final String TOKEN_METADATA_PREFIX = "token:metadata:";
    private static final int ACTIVE_TOKEN_LIMIT = 100; // 동시에 활성화할 수 있는 토큰 수
    private static final int TOKEN_TTL_SECONDS = 600; // 10 minutes

    private final RedisTemplate<String, String> redisTemplate;

    public ReservationTokenRedisAdapter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Optional<ReservationToken> findLatestByUserId(String userId) {
        // Redis Hash에서 사용자의 최신 토큰 찾기
        String token = redisTemplate.opsForHash().get(ACTIVE_TOKENS_KEY, userId) != null 
            ? (String) redisTemplate.opsForHash().get(ACTIVE_TOKENS_KEY, userId)
            : null;
        
        if (token != null) {
            return findByToken(token);
        }
        
        // Active에 없으면 Waiting에서 찾기
        Long rank = redisTemplate.opsForZSet().rank(WAITING_QUEUE_KEY, userId);
        if (rank != null) {
            // Waiting 큐에서 사용자 토큰 찾기 (metadata에서)
            String metadataKey = TOKEN_METADATA_PREFIX + userId;
            String tokenValue = redisTemplate.opsForValue().get(metadataKey);
            if (tokenValue != null) {
                return findByToken(tokenValue);
            }
        }
        
        return Optional.empty();
    }

    @Override
    public Optional<ReservationToken> findByToken(String token) {
        // Token metadata에서 정보 조회
        String userId = findUserIdByToken(token);
        if (userId == null) {
            return Optional.empty();
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        // Active 토큰 확인
        if (Boolean.TRUE.equals(redisTemplate.opsForHash().hasKey(ACTIVE_TOKENS_KEY, userId))) {
            String activeToken = (String) redisTemplate.opsForHash().get(ACTIVE_TOKENS_KEY, userId);
            if (token.equals(activeToken)) {
                LocalDateTime expiresAt = now.plusSeconds(TOKEN_TTL_SECONDS);
                return Optional.of(ReservationToken.rebuild(
                    null,
                    userId,
                    token,
                    ReservationTokenStatus.ACTIVE,
                    calculatePosition(userId),
                    expiresAt,
                    now
                ));
            }
        }
        
        // Waiting 큐 확인
        Long rank = redisTemplate.opsForZSet().rank(WAITING_QUEUE_KEY, userId);
        if (rank != null) {
            String metadataKey = TOKEN_METADATA_PREFIX + userId;
            String storedToken = redisTemplate.opsForValue().get(metadataKey);
            if (token.equals(storedToken)) {
                int position = rank.intValue() + 1;
                Double score = redisTemplate.opsForZSet().score(WAITING_QUEUE_KEY, userId);
                LocalDateTime createdAt = score != null 
                    ? LocalDateTime.ofEpochSecond(score.longValue() / 1000, 0, ZoneOffset.UTC)
                    : now;
                LocalDateTime expiresAt = createdAt.plusSeconds(TOKEN_TTL_SECONDS);
                
                return Optional.of(ReservationToken.rebuild(
                    null,
                    userId,
                    token,
                    ReservationTokenStatus.WAITING,
                    position,
                    expiresAt,
                    createdAt
                ));
            }
        }
        
        return Optional.empty();
    }

    @Override
    public ReservationToken save(ReservationToken token) {
        String userId = token.getUserId();
        String tokenValue = token.getToken();
        
        if (token.getStatus() == ReservationTokenStatus.ACTIVE) {
            // Active 토큰으로 등록
            redisTemplate.opsForHash().put(ACTIVE_TOKENS_KEY, userId, tokenValue);
            redisTemplate.expire(ACTIVE_TOKENS_KEY, java.time.Duration.ofSeconds(TOKEN_TTL_SECONDS));
            
            // Metadata 저장
            String metadataKey = TOKEN_METADATA_PREFIX + userId;
            redisTemplate.opsForValue().set(metadataKey, tokenValue, 
                java.time.Duration.ofSeconds(TOKEN_TTL_SECONDS));
            
            // Waiting 큐에서 제거 (있다면)
            redisTemplate.opsForZSet().remove(WAITING_QUEUE_KEY, userId);
            
            // Active 토큰 수 제한 관리
            promoteFromWaitingQueue();
        } else if (token.getStatus() == ReservationTokenStatus.WAITING) {
            // Waiting 큐에 추가
            long timestampMs = token.getCreatedAt().toEpochSecond(ZoneOffset.UTC) * 1000;
            redisTemplate.opsForZSet().add(WAITING_QUEUE_KEY, userId, timestampMs);
            redisTemplate.expire(WAITING_QUEUE_KEY, java.time.Duration.ofDays(1));
            
            // Metadata 저장
            String metadataKey = TOKEN_METADATA_PREFIX + userId;
            redisTemplate.opsForValue().set(metadataKey, tokenValue,
                java.time.Duration.ofSeconds(TOKEN_TTL_SECONDS));
            
            // Waiting에서 Active로 승격 시도
            promoteFromWaitingQueue();
            
            // 승격 후 상태 확인 - Active로 승격되었는지 확인
            boolean isActive = Boolean.TRUE.equals(redisTemplate.opsForHash().hasKey(ACTIVE_TOKENS_KEY, userId));
            if (isActive) {
                // Active로 승격됨 - 새로운 객체 반환
                return ReservationToken.rebuild(
                    null,
                    userId,
                    tokenValue,
                    ReservationTokenStatus.ACTIVE,
                    0, // Active는 position 0
                    token.getExpiresAt(),
                    token.getCreatedAt()
                );
            }
        } else if (token.getStatus() == ReservationTokenStatus.EXPIRED 
                || token.getStatus() == ReservationTokenStatus.DONE) {
            // 토큰 만료 또는 완료 처리
            redisTemplate.opsForHash().delete(ACTIVE_TOKENS_KEY, userId);
            redisTemplate.opsForZSet().remove(WAITING_QUEUE_KEY, userId);
            String metadataKey = TOKEN_METADATA_PREFIX + userId;
            redisTemplate.delete(metadataKey);
            
            // Active 토큰이 비워졌으므로 Waiting에서 승격
            promoteFromWaitingQueue();
        }
        
        return token;
    }

    /**
     * Waiting 큐에서 Active로 승격
     */
    private void promoteFromWaitingQueue() {
        long activeCount = redisTemplate.opsForHash().size(ACTIVE_TOKENS_KEY);
        
        while (activeCount < ACTIVE_TOKEN_LIMIT) {
            // 가장 오래된 사용자를 가져옴 (score가 가장 낮은 것)
            List<String> topUsers = redisTemplate.opsForZSet()
                .range(WAITING_QUEUE_KEY, 0, 0)
                .stream()
                .map(Object::toString)
                .toList();
            
            if (topUsers.isEmpty()) {
                break;
            }
            
            String userId = topUsers.get(0);
            String metadataKey = TOKEN_METADATA_PREFIX + userId;
            String tokenValue = redisTemplate.opsForValue().get(metadataKey);
            
            if (tokenValue != null) {
                // Active로 이동
                redisTemplate.opsForHash().put(ACTIVE_TOKENS_KEY, userId, tokenValue);
                redisTemplate.opsForZSet().remove(WAITING_QUEUE_KEY, userId);
                activeCount++;
            } else {
                // Metadata가 없으면 Waiting 큐에서 제거
                redisTemplate.opsForZSet().remove(WAITING_QUEUE_KEY, userId);
            }
        }
    }

    /**
     * 사용자의 현재 대기열 위치 계산
     */
    private int calculatePosition(String userId) {
        Long rank = redisTemplate.opsForZSet().rank(WAITING_QUEUE_KEY, userId);
        if (rank != null) {
            return rank.intValue() + 1;
        }
        
        // Active에 있는 경우
        if (Boolean.TRUE.equals(redisTemplate.opsForHash().hasKey(ACTIVE_TOKENS_KEY, userId))) {
            return 0; // Active는 position 0
        }
        
        return -1;
    }

    /**
     * 토큰으로 사용자 ID 찾기
     */
    private String findUserIdByToken(String token) {
        // Active 토큰에서 찾기
        var activeTokens = redisTemplate.opsForHash().entries(ACTIVE_TOKENS_KEY);
        for (var entry : activeTokens.entrySet()) {
            if (token.equals(entry.getValue())) {
                return entry.getKey().toString();
            }
        }
        
        // Waiting 큐의 모든 사용자를 확인
        var waitingUsers = redisTemplate.opsForZSet().range(WAITING_QUEUE_KEY, 0, -1);
        if (waitingUsers != null) {
            for (Object userIdObj : waitingUsers) {
                String userId = userIdObj.toString();
                String metadataKey = TOKEN_METADATA_PREFIX + userId;
                String storedToken = redisTemplate.opsForValue().get(metadataKey);
                if (token.equals(storedToken)) {
                    return userId;
                }
            }
        }
        
        return null;
    }
}
