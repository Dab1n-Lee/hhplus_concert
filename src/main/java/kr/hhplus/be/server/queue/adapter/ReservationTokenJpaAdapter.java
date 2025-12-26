package kr.hhplus.be.server.queue.adapter;

import java.util.Optional;
import kr.hhplus.be.server.queue.adapter.jpa.ReservationTokenEntity;
import kr.hhplus.be.server.queue.adapter.jpa.ReservationTokenJpaRepository;
import kr.hhplus.be.server.queue.domain.ReservationToken;
import kr.hhplus.be.server.queue.port.ReservationTokenRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@ConditionalOnProperty(name = "queue.use-jpa", havingValue = "true", matchIfMissing = false)
@Component
public class ReservationTokenJpaAdapter implements ReservationTokenRepository {
    private final ReservationTokenJpaRepository reservationTokenJpaRepository;

    public ReservationTokenJpaAdapter(ReservationTokenJpaRepository reservationTokenJpaRepository) {
        this.reservationTokenJpaRepository = reservationTokenJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReservationToken> findLatestByUserId(String userId) {
        return reservationTokenJpaRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
            .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReservationToken> findByToken(String token) {
        return reservationTokenJpaRepository.findByToken(token)
            .map(this::toDomain);
    }

    @Override
    @Transactional
    public ReservationToken save(ReservationToken token) {
        ReservationTokenEntity entity;
        if (token.getId() == null) {
            entity = new ReservationTokenEntity(
                token.getUserId(),
                token.getToken(),
                token.getStatus(),
                token.getPosition(),
                token.getExpiresAt(),
                token.getCreatedAt()
            );
        } else {
            entity = reservationTokenJpaRepository.findById(token.getId())
                .orElseThrow(() -> new IllegalStateException("Reservation token not found."));
            entity.updateStatus(token.getStatus(), token.getExpiresAt());
        }

        ReservationTokenEntity saved = reservationTokenJpaRepository.save(entity);
        token.setId(saved.getId());
        return token;
    }

    private ReservationToken toDomain(ReservationTokenEntity entity) {
        return ReservationToken.rebuild(
            entity.getId(),
            entity.getUserId(),
            entity.getToken(),
            entity.getStatus(),
            entity.getPosition(),
            entity.getExpiresAt(),
            entity.getCreatedAt()
        );
    }
}
