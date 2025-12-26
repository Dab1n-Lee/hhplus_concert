# Week 7 과제 보고서 - Redis 기반 대용량 트래픽 처리

## 과제 개요

본 과제는 Redis를 활용하여 대용량 트래픽을 효율적으로 처리하기 위한 두 가지 기능을 구현하는 것입니다.

- **필수과제**: Ranking Design - 콘서트 예약 시나리오의 빠른 매진 랭킹 구현
- **선택과제**: Asynchronous Design - 콘서트 예약 시나리오의 대기열 기능 Redis 기반 개선

## 1. 필수과제: Ranking Design - 빠른 매진 랭킹

### 1.1 설계 목표

콘서트 날짜별로 빠르게 매진된 순서를 실시간으로 추적하고 랭킹을 제공하는 기능을 구현합니다. 이를 통해 인기 있는 콘서트 날짜를 파악하고 사용자에게 제공할 수 있습니다.

### 1.2 기술 스택 및 선택 이유

**Redis Sorted Set**을 사용하여 랭킹을 구현했습니다.

**선택 이유**:
- **O(log N) 조회 성능**: 대량의 데이터에서도 빠른 랭킹 조회가 가능
- **원자성 보장**: `ZINCRBY` 명령어를 사용하여 동시성 문제 없이 점수 증가
- **정렬된 데이터 구조**: Score 기반 자동 정렬로 추가 정렬 작업 불필요
- **TTL 지원**: 자동 만료 기능으로 메모리 관리 용이

### 1.3 구현 내용

#### 1.3.1 ConcertRankingService

```java
@Service
public class ConcertRankingService {
    private static final String RANKING_KEY = "concert:sold-out-ranking";
    
    // 결제 완료 시 예약 완료 좌석 수 증가
    public void incrementSoldOutCount(LocalDate concertDate) {
        String dateKey = concertDate.toString();
        redisTemplate.opsForZSet().incrementScore(RANKING_KEY, dateKey, 1);
    }
    
    // 상위 랭킹 조회
    public List<ConcertRanking> getTopRanking(int limit) {
        // ZREVRANGE를 사용하여 높은 점수 순으로 조회
    }
}
```

**주요 기능**:
- `incrementSoldOutCount`: 결제 완료 시 `ZINCRBY` 명령으로 해당 날짜의 점수 증가
- `getTopRanking`: `ZREVRANGE`를 사용하여 상위 N개 랭킹 조회
- `getSoldOutCount`: 특정 날짜의 예약 완료 좌석 수 조회
- `getRank`: 특정 날짜의 랭킹 순위 조회

#### 1.3.2 PayReservationUseCase 통합

결제 완료 시점에 랭킹을 업데이트하도록 통합했습니다.

```java
// 결제 완료 후 랭킹 업데이트
concertRankingService.incrementSoldOutCount(seat.getConcertDate());
```

#### 1.3.3 API 엔드포인트

```
GET /api/v1/concerts/ranking?limit=10
```

응답 예시:
```json
[
  {
    "concertDate": "2025-01-15",
    "soldOutCount": 150,
    "rank": 1
  },
  {
    "concertDate": "2025-01-14",
    "soldOutCount": 120,
    "rank": 2
  }
]
```

### 1.4 성능 및 특징

- **조회 성능**: O(log N) - Sorted Set의 특성상 매우 빠른 조회
- **동시성**: Redis의 원자적 연산으로 동시성 문제 해결
- **확장성**: 대량의 콘서트 날짜 데이터에서도 일정한 성능 유지
- **메모리 효율**: TTL 설정으로 오래된 데이터 자동 정리 (30일)

## 2. 선택과제: Asynchronous Design - 대기열 기능 Redis 기반 개선

### 2.1 설계 목표

기존 JPA 기반 대기열 시스템을 Redis 기반으로 개선하여, 대용량 트래픽 상황에서도 효율적으로 대기열을 관리하고 사용자에게 실시간 대기 순서를 제공합니다.

### 2.2 기술 스택 및 선택 이유

**Redis Sorted Set + Hash**를 사용하여 대기열을 구현했습니다.

**선택 이유**:
- **Sorted Set**: 신청 시각을 score로 사용하여 선착순 관리, O(log N) 조회 성능
- **Hash**: Active 토큰을 빠르게 조회하고 관리하기 위해 사용
- **메모리 기반**: 빠른 조회 및 업데이트 성능
- **원자성 보장**: Redis 명령어의 원자성으로 동시성 문제 해결

### 2.3 구현 내용

#### 2.3.1 데이터 구조 설계

1. **Waiting Queue (Sorted Set)**
   - Key: `queue:waiting`
   - Member: userId
   - Score: 신청 시각 (timestamp in milliseconds)
   - 용도: 대기 중인 사용자들의 선착순 관리

2. **Active Tokens (Hash)**
   - Key: `queue:active`
   - Field: userId
   - Value: token
   - 용도: 현재 활성화된 토큰 관리 (최대 100개)

3. **Token Metadata (String)**
   - Key: `token:metadata:{userId}`
   - Value: token
   - TTL: 10분
   - 용도: 사용자별 토큰 정보 저장

#### 2.3.2 ReservationTokenRedisAdapter

```java
@Primary
@Component
public class ReservationTokenRedisAdapter implements ReservationTokenRepository {
    
    @Override
    public ReservationToken save(ReservationToken token) {
        if (token.getStatus() == ReservationTokenStatus.WAITING) {
            // Waiting 큐에 추가 (Sorted Set)
            long timestampMs = token.getCreatedAt().toEpochSecond(ZoneOffset.UTC) * 1000;
            redisTemplate.opsForZSet().add(WAITING_QUEUE_KEY, userId, timestampMs);
            
            // Active로 승격 시도
            promoteFromWaitingQueue();
        } else if (token.getStatus() == ReservationTokenStatus.ACTIVE) {
            // Active 토큰으로 등록 (Hash)
            redisTemplate.opsForHash().put(ACTIVE_TOKENS_KEY, userId, tokenValue);
            redisTemplate.opsForZSet().remove(WAITING_QUEUE_KEY, userId);
        }
    }
    
    private void promoteFromWaitingQueue() {
        // Active 토큰 수가 100개 미만이면 Waiting에서 가장 오래된 사용자를 Active로 승격
        while (activeCount < ACTIVE_TOKEN_LIMIT) {
            List<String> topUsers = redisTemplate.opsForZSet()
                .range(WAITING_QUEUE_KEY, 0, 0); // 가장 오래된 사용자
            // ... 승격 로직
        }
    }
}
```

**주요 기능**:
- **토큰 발급**: WAITING 상태로 시작, 가능하면 즉시 ACTIVE로 승격
- **대기열 관리**: Sorted Set을 사용한 선착순 관리
- **Active 제한**: 동시 Active 토큰 수를 100개로 제한하여 서버 부하 관리
- **자동 승격**: Active 토큰이 비워지면 Waiting에서 자동으로 승격

#### 2.3.3 ReservationTokenService 개선

```java
public ReservationToken issue(String userId) {
    // 기존 토큰 확인
    var existing = reservationTokenRepository.findLatestByUserId(userId);
    if (existing.isPresent() && !existing.get().isExpired(now)) {
        return existing.get();
    }
    
    // 새 토큰을 WAITING 상태로 생성
    ReservationToken newToken = ReservationToken.rebuild(
        null, userId, tokenValue,
        ReservationTokenStatus.WAITING, 0,
        expiresAt, createdAt
    );
    
    // Redis 어댑터가 WAITING 큐에 추가하고 필요시 ACTIVE로 승격
    return reservationTokenRepository.save(newToken);
}
```

### 2.4 성능 및 특징

- **조회 성능**: 
  - Waiting 큐 조회: O(log N)
  - Active 토큰 조회: O(1) - Hash 조회
- **동시성**: Redis의 원자적 연산으로 동시성 문제 해결
- **확장성**: 메모리 기반으로 빠른 처리 속도
- **부하 관리**: Active 토큰 수 제한으로 서버 부하 제어
- **실시간성**: 사용자의 대기 순서를 실시간으로 제공 가능

### 2.5 기존 시스템과의 비교

| 항목 | JPA 기반 (기존) | Redis 기반 (개선) |
|------|----------------|-------------------|
| 조회 성능 | O(N) - DB 쿼리 | O(log N) - Sorted Set |
| 동시성 처리 | DB 락 필요 | Redis 원자적 연산 |
| 대기열 관리 | 복잡한 쿼리 필요 | Sorted Set으로 간단 |
| 확장성 | DB 부하 증가 | 메모리 기반으로 빠름 |
| 실시간성 | 제한적 | 우수 |

## 3. 회고 및 개선 사항

### 3.1 구현 과정에서의 어려움

1. **대기열 상태 전환 로직**
   - WAITING → ACTIVE 전환이 비동기적으로 발생할 수 있어 상태 일관성 유지가 어려웠음
   - 해결: Redis 어댑터 내에서 자동 승격 로직을 구현하여 일관성 유지

2. **Position 계산**
   - ReservationToken의 position 필드가 final이라 동적으로 업데이트 불가
   - 해결: rebuild 메서드를 사용하여 새로운 객체 생성

### 3.2 개선 가능한 부분

1. **대기열 스케줄러**
   - 현재는 토큰 발급 시점에만 승격이 일어남
   - 개선: 주기적으로 실행되는 스케줄러를 추가하여 더 정확한 대기열 관리

2. **TTL 관리**
   - Active 토큰의 TTL이 각각 관리되지 않음
   - 개선: 각 토큰별로 개별 TTL 설정하여 더 정확한 만료 처리

3. **에러 처리**
   - Redis 연결 실패 시 처리 로직 부족
   - 개선: Circuit Breaker 패턴 적용 또는 Fallback 로직 추가

4. **모니터링**
   - 대기열 상태 모니터링 부족
   - 개선: Active/Waiting 토큰 수, 평균 대기 시간 등의 메트릭 수집

### 3.3 학습 내용

1. **Redis Sorted Set의 활용**
   - 랭킹 시스템과 대기열 시스템 모두에 효과적으로 활용 가능
   - Score를 다양한 의미로 사용할 수 있음을 학습

2. **대용량 트래픽 처리**
   - 메모리 기반 저장소의 중요성과 성능 차이를 체감
   - DB 부하를 줄이고 Redis로 오프로드하는 전략의 효과

3. **아키텍처 설계**
   - Repository 패턴을 통한 구현체 교체의 용이성
   - @Primary와 @ConditionalOnProperty를 통한 전략 선택

### 3.4 향후 계획

1. **성능 테스트**
   - 실제 대용량 트래픽 시나리오에서의 성능 측정
   - 동시성 테스트 강화

2. **모니터링 강화**
   - Prometheus + Grafana를 통한 대기열 메트릭 시각화
   - 알람 설정

3. **문서화**
   - API 문서화 (Swagger)
   - 운영 가이드 작성

## 4. 결론

Redis를 활용하여 대용량 트래픽을 효율적으로 처리할 수 있는 두 가지 핵심 기능을 구현했습니다. 

- **랭킹 시스템**: 실시간으로 빠른 매진 랭킹을 제공하여 사용자에게 인기 콘서트 정보 제공
- **대기열 시스템**: Redis 기반으로 대기열을 관리하여 서버 부하를 효과적으로 분산

두 기능 모두 Redis의 특성을 최대한 활용하여 성능과 확장성을 확보했으며, 향후 개선 사항을 통해 더욱 안정적이고 효율적인 시스템으로 발전시킬 수 있을 것입니다.
