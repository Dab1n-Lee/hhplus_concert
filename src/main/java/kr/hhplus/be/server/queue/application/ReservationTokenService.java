package kr.hhplus.be.server.queue.application;

import java.time.LocalDateTime;
import java.util.UUID;
import kr.hhplus.be.server.queue.domain.ReservationToken;
import kr.hhplus.be.server.queue.domain.ReservationTokenStatus;
import kr.hhplus.be.server.queue.port.ReservationTokenRepository;
import kr.hhplus.be.server.reservation.port.ClockProvider;
import org.springframework.stereotype.Service;

@Service
public class ReservationTokenService {
    private static final int TOKEN_TTL_MINUTES = 10;

    private final ReservationTokenRepository reservationTokenRepository;
    private final ClockProvider clockProvider;

    public ReservationTokenService(
        ReservationTokenRepository reservationTokenRepository,
        ClockProvider clockProvider
    ) {
        this.reservationTokenRepository = reservationTokenRepository;
        this.clockProvider = clockProvider;
    }

    public ReservationToken issue(String userId) {
        LocalDateTime now = clockProvider.now();
        var existing = reservationTokenRepository.findLatestByUserId(userId);
        if (existing.isPresent()) {
            ReservationToken token = existing.get();
            if (!token.isExpired(now) && token.isActiveOrWaiting()) {
                return token;
            }
            if (token.isExpired(now) && token.getStatus() != ReservationTokenStatus.EXPIRED) {
                token.expire(now);
                reservationTokenRepository.save(token);
            }
        }

        // 새 토큰을 WAITING 상태로 생성 (Redis 어댑터가 처리함)
        String tokenValue = UUID.randomUUID().toString();
        LocalDateTime createdAt = now;
        
        // WAITING 상태로 토큰 생성
        ReservationToken newToken = ReservationToken.rebuild(
            null,
            userId,
            tokenValue,
            ReservationTokenStatus.WAITING,
            0, // position은 save 후 계산됨
            now.plusMinutes(TOKEN_TTL_MINUTES),
            createdAt
        );
        
        // Redis 어댑터가 WAITING 큐에 추가하고 필요시 ACTIVE로 승격
        return reservationTokenRepository.save(newToken);
    }

    public ReservationToken validateActive(String tokenValue) {
        LocalDateTime now = clockProvider.now();
        ReservationToken token = reservationTokenRepository.findByToken(tokenValue)
            .orElseThrow(() -> new IllegalStateException("Queue token not found."));

        if (token.isExpired(now)) {
            token.expire(now);
            reservationTokenRepository.save(token);
            throw new IllegalStateException("Queue token expired.");
        }

        if (token.getStatus() != ReservationTokenStatus.ACTIVE) {
            throw new IllegalStateException("Queue token is not active.");
        }

        return token;
    }

    public void complete(String tokenValue) {
        ReservationToken token = validateActive(tokenValue);
        token.complete();
        reservationTokenRepository.save(token);
    }
}
