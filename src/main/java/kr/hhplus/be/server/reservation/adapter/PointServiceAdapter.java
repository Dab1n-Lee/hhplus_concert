package kr.hhplus.be.server.reservation.adapter;

import kr.hhplus.be.server.point.service.PointService;
import kr.hhplus.be.server.reservation.port.UserBalanceRepository;
import org.springframework.stereotype.Component;

@Component
public class PointServiceAdapter implements UserBalanceRepository {
    private final PointService pointService;

    public PointServiceAdapter(PointService pointService) {
        this.pointService = pointService;
    }

    @Override
    public long getBalance(String userId) {
        return pointService.getPoint(userId).getBalance();
    }

    @Override
    public void use(String userId, long amount) {
        pointService.use(userId, amount);
    }
}
