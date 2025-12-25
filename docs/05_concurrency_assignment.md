# 콘서트 예약 서비스 동시성 제어 과제 보고서

## 1. 문제 상황

콘서트 예약 서비스에서 다음과 같은 동시성 이슈가 발생할 수 있습니다:

### 1.1 같은 좌석에 대한 동시 예약 요청
- **문제**: 여러 사용자가 동시에 같은 좌석을 예약하려고 할 때 중복 예약이 발생할 수 있음
- **영향**: 한 좌석이 여러 사용자에게 배정되어 데이터 정합성 문제 발생

### 1.2 잔액 차감 중 충돌 발생
- **문제**: 동일 사용자가 동시에 여러 결제 요청을 보낼 때, 잔액 확인과 차감 사이에 다른 트랜잭션이 끼어들어 음수 잔액 발생 가능
- **영향**: 포인트가 음수로 떨어져 비즈니스 로직 위반 및 금전적 손실 가능

### 1.3 예약 후 결제 지연으로 인한 임시 배정 해제 로직 부정확
- **문제**: 좌석 임시 배정(HOLD) 후 결제가 완료되지 않으면 일정 시간 후 자동 해제되어야 하는데, 수동으로만 처리되거나 처리되지 않음
- **영향**: 만료된 예약이 좌석을 계속 점유하여 다른 사용자가 예약할 수 없음

## 2. 해결 전략

### 2.1 좌석 임시 배정 시 락 제어 (SELECT FOR UPDATE)

**해결 방법**: 비관적 락(Pessimistic Lock)을 사용하여 동시 예약 방지

**구현 내용**:
- `SeatRepository.findForUpdateByDateAndSeatNumber()` 메서드에서 `@Lock(LockModeType.PESSIMISTIC_WRITE)` 사용
- `SELECT ... FOR UPDATE` 쿼리를 통해 좌석 조회 시 배타적 락 획득
- `ReserveSeatUseCase`에서 `seatPort.loadForUpdate()`를 사용하여 좌석 조회 및 상태 변경을 원자적으로 처리

**코드 위치**:
- `src/main/java/kr/hhplus/be/server/concert/repository/SeatRepository.java`
- `src/main/java/kr/hhplus/be/server/reservation/application/ReserveSeatUseCase.java`

**동작 원리**:
1. 트랜잭션 시작
2. `SELECT ... FOR UPDATE`로 좌석 조회 시 해당 행에 X-Lock(배타적 락) 획득
3. 다른 트랜잭션은 해당 좌석에 접근 불가 (읽기/쓰기 모두 대기)
4. 좌석 상태 확인 및 HOLD 상태로 변경
5. 트랜잭션 커밋 시 락 해제

### 2.2 잔액 차감 동시성 제어 (조건부 UPDATE)

**해결 방법**: 조건부 UPDATE를 사용하여 잔액 확인과 차감을 원자적으로 처리

**구현 내용**:
- `UserPointRepository.deductIfSufficient()` 메서드 추가
- `UPDATE user_point SET balance = balance - :amount WHERE user_id = :userId AND balance >= :amount` 쿼리 사용
- 업데이트된 행 수가 0이면 잔액 부족으로 처리
- `PointService.use()`에서 조건부 UPDATE 사용
- `PayReservationUseCase`에서 `getBalance()` 호출 제거하여 동시성 문제 해결

**코드 위치**:
- `src/main/java/kr/hhplus/be/server/point/repository/UserPointRepository.java`
- `src/main/java/kr/hhplus/be/server/point/service/PointService.java`
- `src/main/java/kr/hhplus/be/server/reservation/application/PayReservationUseCase.java`

**동작 원리**:
1. 조건부 UPDATE 쿼리 실행: `balance >= amount` 조건을 만족하는 경우에만 차감
2. 업데이트된 행 수 확인:
   - 1: 차감 성공 (잔액 충분)
   - 0: 차감 실패 (잔액 부족 또는 사용자 없음)
3. 원자적 연산이므로 다른 트랜잭션이 끼어들 수 없음

### 2.3 배정 타임아웃 해제 스케줄러

**해결 방법**: Spring의 `@Scheduled`를 사용하여 주기적으로 만료된 예약을 찾아 해제

**구현 내용**:
- `ReservationExpirationScheduler` 클래스 생성
- `@Scheduled(fixedRate = 60000)` 어노테이션으로 1분마다 실행
- 만료된 HOLD 상태의 예약을 조회하여:
  1. 예약 상태를 EXPIRED로 변경
  2. 해당 좌석의 HOLD 상태를 해제하여 AVAILABLE로 변경
- `ReservationJpaRepository.findExpiredHolds()` 메서드 추가

**코드 위치**:
- `src/main/java/kr/hhplus/be/server/reservation/scheduler/ReservationExpirationScheduler.java`
- `src/main/java/kr/hhplus/be/server/reservation/adapter/jpa/ReservationJpaRepository.java`
- `src/main/java/kr/hhplus/be/server/ServerApplication.java` (@EnableScheduling 추가)

**동작 원리**:
1. 스케줄러가 1분마다 실행
2. 현재 시간 기준으로 만료된 HOLD 상태의 예약 조회
3. 각 만료된 예약에 대해:
   - 예약 상태를 EXPIRED로 변경
   - 해당 좌석을 조회하여 HOLD 상태 해제
4. 트랜잭션 내에서 처리하여 일관성 보장

## 3. 테스트 결과

### 3.1 좌석 동시 예약 방지 테스트

**테스트 파일**: `ReservationConcurrencyIntegrationTest.allowsOnlyOneReservationForSameSeatConcurrently()`

**테스트 시나리오**:
- 10개의 스레드가 동시에 같은 좌석(좌석 번호 10)을 예약 시도
- 초기 상태: 좌석은 AVAILABLE 상태

**결과**:
- ✅ 성공한 예약: 1개
- ✅ 실패한 예약: 9개
- ✅ 최종 예약 수: 1개
- ✅ 좌석 상태: HELD로 변경됨

**검증 내용**:
- SELECT FOR UPDATE를 통한 비관적 락이 제대로 작동하여 동시에 여러 요청이 와도 하나만 성공
- 좌석 상태가 정확히 HELD로 변경됨

### 3.2 잔액 차감 동시성 테스트

**테스트 파일**: `BalanceDeductionConcurrencyTest`

#### 테스트 1: 여러 결제 요청 시 잔액 음수 방지
**테스트 시나리오**:
- 초기 잔액: 1000 포인트
- 5개의 좌석 예약 후 동시에 각각 100 포인트씩 결제 요청

**결과**:
- ✅ 성공한 결제: 5개
- ✅ 최종 잔액: 500 포인트 (1000 - 500)
- ✅ 잔액이 음수가 되지 않음

#### 테스트 2: 잔액 부족 시 과도한 차감 방지
**테스트 시나리오**:
- 초기 잔액: 1000 포인트
- 3개의 좌석 예약 후 동시에 각각 500 포인트씩 결제 요청 (최대 2개만 가능)

**결과**:
- ✅ 성공한 결제: 2개 이하
- ✅ 최종 잔액: 0 이상
- ✅ 잔액이 음수가 되지 않음

**검증 내용**:
- 조건부 UPDATE를 통한 원자적 연산이 제대로 작동
- 동시에 여러 결제 요청이 와도 잔액이 음수로 떨어지지 않음

### 3.3 배정 타임아웃 해제 스케줄러 테스트

**테스트 파일**: `ReservationExpirationSchedulerTest`

#### 테스트 1: 만료된 예약 해제
**테스트 시나리오**:
- 만료된 HOLD 상태의 예약 1개와 유효한 HOLD 상태의 예약 1개 생성
- 스케줄러 실행

**결과**:
- ✅ 만료된 예약: EXPIRED로 변경됨
- ✅ 만료된 좌석: AVAILABLE로 변경됨
- ✅ 유효한 예약: HOLD 상태 유지
- ✅ 유효한 좌석: HELD 상태 유지

#### 테스트 2: 여러 만료된 예약 일괄 처리
**테스트 시나리오**:
- 만료된 HOLD 상태의 예약 5개 생성
- 스케줄러 실행

**결과**:
- ✅ 모든 만료된 예약이 EXPIRED로 변경됨
- ✅ 모든 만료된 좌석이 AVAILABLE로 변경됨

#### 테스트 3: 만료된 예약이 없을 때
**테스트 시나리오**:
- 유효한 HOLD 상태의 예약만 존재
- 스케줄러 실행

**결과**:
- ✅ 예약 상태 변경 없음
- ✅ 좌석 상태 변경 없음

**검증 내용**:
- 스케줄러가 정확히 만료된 예약만 찾아서 처리
- 좌석 상태가 정확히 해제됨
- 유효한 예약에는 영향 없음

## 4. 사용된 동시성 제어 기법

### 4.1 SELECT FOR UPDATE (비관적 락)
- **사용 위치**: 좌석 예약 시
- **장점**: 충돌 완전 차단, 구현 단순
- **단점**: 락 대기 시간으로 인한 성능 저하 가능, 데드락 위험

### 4.2 조건부 UPDATE
- **사용 위치**: 잔액 차감 시
- **장점**: 원자적 연산, 락 대기 없음, 성능 우수
- **단점**: 업데이트 실패 시 재시도 로직 필요 (현재는 예외 발생)

### 4.3 스케줄러 기반 정리 작업
- **사용 위치**: 만료된 예약 해제
- **장점**: 주기적 자동 처리, 시스템 부하 분산
- **단점**: 실시간 처리가 아닌 주기적 처리 (최대 1분 지연)

## 5. 개선 사항 및 고려사항

### 5.1 트랜잭션 격리 수준
- 현재 기본 격리 수준 사용 (READ COMMITTED)
- 좌석 예약 시 REPEATABLE READ 격리 수준 고려 가능
- 성능과 정합성의 균형 고려 필요

### 5.2 스케줄러 실행 주기
- 현재 1분마다 실행
- 트래픽이 많은 경우 더 짧은 주기로 조정 가능
- 단, DB 부하 고려 필요

### 5.3 분산 환경 고려
- 현재 단일 인스턴스 기준으로 구현
- 다중 인스턴스 환경에서는 스케줄러 중복 실행 방지 필요 (분산 락 등)

## 6. 결론

콘서트 예약 서비스의 주요 동시성 이슈 3가지를 다음과 같이 해결했습니다:

1. **좌석 동시 예약 방지**: SELECT FOR UPDATE를 통한 비관적 락으로 완전 차단
2. **잔액 차감 동시성 제어**: 조건부 UPDATE를 통한 원자적 연산으로 음수 잔액 방지
3. **배정 타임아웃 해제**: 스케줄러를 통한 주기적 자동 처리

모든 해결책은 멀티스레드 테스트를 통해 검증되었으며, 동시성 문제가 발생하지 않음을 확인했습니다.
