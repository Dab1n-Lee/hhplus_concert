# ERD

```mermaid
erDiagram
    USERS ||--o{ QUEUE_TOKENS : has
    USERS ||--o{ POINT_HISTORIES : has
    USERS ||--o{ RESERVATIONS : places
    USERS ||--o{ PAYMENTS : pays

    CONCERTS ||--o{ CONCERT_SCHEDULES : has
    CONCERT_SCHEDULES ||--o{ SEATS : has
    CONCERT_SCHEDULES ||--o{ RESERVATIONS : has

    SEATS ||--o{ RESERVATIONS : held_by
    RESERVATIONS ||--|| PAYMENTS : paid_by

    USERS {
        UUID id PK
        string name
        datetime created_at
    }

    QUEUE_TOKENS {
        UUID id PK
        UUID user_id FK
        string token
        string status
        int position
        datetime expires_at
        datetime created_at
    }

    POINT_HISTORIES {
        UUID id PK
        UUID user_id FK
        string type
        int amount
        int balance_after
        datetime created_at
    }

    CONCERTS {
        UUID id PK
        string title
        int price
        datetime created_at
    }

    CONCERT_SCHEDULES {
        UUID id PK
        UUID concert_id FK
        date concert_date
        datetime starts_at
        string status
        datetime created_at
    }

    SEATS {
        UUID id PK
        UUID schedule_id FK
        int seat_no
        string status
        datetime hold_expires_at
        datetime created_at
    }

    RESERVATIONS {
        UUID id PK
        UUID user_id FK
        UUID schedule_id FK
        UUID seat_id FK
        string status
        datetime hold_expires_at
        datetime created_at
    }

    PAYMENTS {
        UUID id PK
        UUID reservation_id FK
        UUID user_id FK
        int amount
        string status
        datetime paid_at
        datetime created_at
    }
```

## 테이블 설명

- **USERS**: 서비스 사용자 정보
- **QUEUE_TOKENS**: 대기열 토큰 및 상태(WAITING, ACTIVE, DONE, EXPIRED)
- **POINT_HISTORIES**: 포인트 충전/사용 내역과 잔액 스냅샷
- **CONCERTS**: 콘서트 기본 정보(가격 포함)
- **CONCERT_SCHEDULES**: 공연 날짜별 일정
- **SEATS**: 일정별 좌석 상태(AVAILABLE, HELD, RESERVED)
- **RESERVATIONS**: 좌석 예약 내역(HELD, CONFIRMED, EXPIRED, CANCELLED)
- **PAYMENTS**: 결제 내역

## 제약 및 인덱스

- `SEATS(schedule_id, seat_no)`에 **유니크 제약**으로 좌석 중복 생성 방지
- `RESERVATIONS(seat_id, status)`에 **부분 유니크 제약**(HELD/CONFIRMED)로 동시 예약 방지
- `QUEUE_TOKENS(token)` 인덱스로 토큰 검증 성능 확보
- `POINT_HISTORIES(user_id, created_at)` 인덱스로 잔액 조회 성능 확보
