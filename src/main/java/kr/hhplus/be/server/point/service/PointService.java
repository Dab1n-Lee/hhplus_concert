package kr.hhplus.be.server.point.service;

import kr.hhplus.be.server.point.domain.UserPoint;
import kr.hhplus.be.server.point.repository.UserPointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PointService {
    private final UserPointRepository userPointRepository;

    public PointService(UserPointRepository userPointRepository) {
        this.userPointRepository = userPointRepository;
    }

    @Transactional
    public UserPoint charge(String userId, long amount) {
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
}
