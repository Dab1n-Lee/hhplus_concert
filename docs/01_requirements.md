# 콘서트 예약 서비스 요구사항 및 API 명세

## 시나리오 요약

- 대기열 토큰을 발급받은 사용자만 예약/결제 기능을 이용합니다.
- 좌석 예약 시 사용자는 사전에 충전한 포인트를 사용합니다.
- 좌석 예약 시점에 해당 좌석은 일정 시간(정책값) 동안 임시 배정(HOLD)되며, 결제가 완료되지 않으면 자동 해제됩니다.

## 핵심 정책

- **대기열 통과**: 토큰이 유효하고 입장 가능 상태일 때만 API 호출을 허용합니다.
- **좌석 임시 배정**: 예약 요청 시 `HOLD_TTL=5분`으로 좌석을 임시 배정합니다.
- **동시성 제어**: 동일 좌석에 대해 동시 예약이 발생하지 않도록 좌석/예약 테이블에 고유 제약 및 트랜잭션 잠금을 사용합니다.

## API 명세

> 공통 규칙
> - 응답 시간 형식은 ISO-8601 (`2024-12-31T23:59:59Z`)을 사용합니다.
> - 인증/대기열 검증은 `Queue-Token` 헤더로 전달합니다.

### 1) 유저 대기열 토큰 발급

- **POST** `/api/v1/queue/tokens`
- **설명**: 유저의 대기열 토큰을 발급합니다. 이미 유효한 토큰이 있으면 동일 토큰을 반환합니다.
- **요청**
```json
{
  "userId": "uuid"
}
```
- **응답**
```json
{
  "token": "queue-token",
  "status": "WAITING",
  "position": 120,
  "estimatedWaitSec": 600,
  "expiresAt": "2024-12-31T23:59:59Z"
}
```

### 2) 예약 가능 날짜 조회

- **GET** `/api/v1/concerts/{concertId}/dates`
- **설명**: 예약 가능한 날짜 목록을 조회합니다.
- **헤더**: `Queue-Token`
- **응답**
```json
{
  "concertId": "concert-1",
  "availableDates": [
    "2025-01-10",
    "2025-01-11"
  ]
}
```

### 3) 예약 가능 좌석 조회

- **GET** `/api/v1/concerts/{concertId}/dates/{date}/seats`
- **설명**: 특정 날짜의 예약 가능 좌석 목록을 조회합니다. 좌석 번호는 1~50입니다.
- **헤더**: `Queue-Token`
- **응답**
```json
{
  "concertId": "concert-1",
  "date": "2025-01-10",
  "seats": [
    { "seatNo": 1, "status": "AVAILABLE" },
    { "seatNo": 2, "status": "HELD", "holdExpiresAt": "2025-01-10T10:05:00Z" }
  ]
}
```

### 4) 좌석 예약 요청

- **POST** `/api/v1/reservations`
- **설명**: 좌석을 임시 배정(HOLD)합니다.
- **헤더**: `Queue-Token`
- **요청**
```json
{
  "userId": "uuid",
  "concertId": "concert-1",
  "date": "2025-01-10",
  "seatNo": 3
}
```
- **응답**
```json
{
  "reservationId": "reservation-1",
  "status": "HELD",
  "holdExpiresAt": "2025-01-10T10:05:00Z"
}
```

### 5) 포인트 충전

- **POST** `/api/v1/points/charge`
- **설명**: 결제에 사용할 포인트를 충전합니다.
- **요청**
```json
{
  "userId": "uuid",
  "amount": 50000
}
```
- **응답**
```json
{
  "userId": "uuid",
  "balance": 50000,
  "chargedAmount": 50000,
  "transactionId": "balance-1",
  "createdAt": "2025-01-10T09:00:00Z"
}
```

### 6) 포인트 조회

- **GET** `/api/v1/points/{userId}`
- **설명**: 사용자의 현재 포인트 잔액을 조회합니다.
- **응답**
```json
{
  "userId": "uuid",
  "balance": 45000,
  "asOf": "2025-01-10T09:10:00Z"
}
```

### 7) 결제

- **POST** `/api/v1/payments`
- **설명**: 예약 좌석에 대해 결제를 수행하고 예약을 확정합니다.
- **헤더**: `Queue-Token`
- **요청**
```json
{
  "reservationId": "reservation-1",
  "userId": "uuid",
  "amount": 5000
}
```
- **응답**
```json
{
  "paymentId": "payment-1",
  "status": "PAID",
  "paidAt": "2025-01-10T09:12:00Z",
  "seatNo": 3,
  "date": "2025-01-10"
}
```

## 공통 오류 응답

- `400 Bad Request`: 잘못된 요청 파라미터
- `401 Unauthorized`: 대기열 토큰 검증 실패
- `404 Not Found`: 대상 리소스 없음
- `409 Conflict`: 좌석 중복 예약 등 동시성 충돌
