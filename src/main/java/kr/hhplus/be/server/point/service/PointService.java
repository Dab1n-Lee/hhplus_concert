package kr.hhplus.be.server.point.service;

import kr.hhplus.be.server.lock.adapter.redis.SpinDistributedLock;
import kr.hhplus.be.server.point.domain.UserPoint;
import kr.hhplus.be.server.point.repository.UserPointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PointService {
    private static final long LOCK_WAIT_TIME_MS = 1000; // 1 second
    private static final long LOCK_LEASE_TIME_MS = 5000; // 5 seconds

    private final UserPointRepository userPointRepository;
    private final SpinDistributedLock distributedLock;

    public PointService(UserPointRepository userPointRepository, SpinDistributedLock distributedLock) {
        this.userPointRepository = userPointRepository;
        this.distributedLock = distributedLock;
    }

    public UserPoint charge(String userId, long amount) {
        String lockKey = "user:point:charge:" + userId;
        return distributedLock.executeWithLock(
            lockKey,
            LOCK_WAIT_TIME_MS,
            LOCK_LEASE_TIME_MS,
            () -> chargeInternal(userId, amount)
        );
    }

    @Transactional
    private UserPoint chargeInternal(String userId, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Charge amount must be positive.");
        }

        UserPoint userPoint = userPointRepository.findByUserIdForUpdate(userId)
            .orElseGet(() -> userPointRepository.save(new UserPoint(userId, 0L)));

        userPoint.charge(amount);
        return userPoint;
    }

    @Transactional(readOnly = true)
    public UserPoint getPoint(String userId) {
        return userPointRepository.findByUserId(userId)
            .orElseGet(() -> new UserPoint(userId, 0L));
    }

    public UserPoint use(String userId, long amount) {
        String lockKey = "user:point:use:" + userId;
        return distributedLock.executeWithLock(
            lockKey,
            LOCK_WAIT_TIME_MS,
            LOCK_LEASE_TIME_MS,
            () -> useInternal(userId, amount)
        );
    }

    @Transactional
    private UserPoint useInternal(String userId, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Use amount must be positive.");
        }

        // 조건부 UPDATE를 사용하여 원자적으로 잔액 확인 및 차감
        int updatedRows = userPointRepository.deductIfSufficient(userId, amount);
        if (updatedRows == 0) {
            // 업데이트된 행이 없으면 잔액 부족 또는 사용자 없음
            UserPoint userPoint = userPointRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("User point not found."));
            throw new IllegalStateException("Insufficient points.");
        }

        return userPointRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalStateException("User point not found."));
    }
}
