package kr.hhplus.be.server.queue.application;

import java.time.LocalDateTime;
import java.util.UUID;
import kr.hhplus.be.server.queue.domain.ReservationToken;
import kr.hhplus.be.server.queue.domain.ReservationTokenStatus;
import kr.hhplus.be.server.queue.port.ReservationTokenRepository;
import kr.hhplus.be.server.reservation.port.ClockProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
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

        ReservationToken newToken = ReservationToken.issue(
            userId,
            UUID.randomUUID().toString(),
            1,
            now.plusMinutes(TOKEN_TTL_MINUTES),
            now
        );
        return reservationTokenRepository.save(newToken);
    }

    @Transactional
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

    @Transactional
    public void complete(String tokenValue) {
        ReservationToken token = validateActive(tokenValue);
        token.complete();
        reservationTokenRepository.save(token);
    }
}
