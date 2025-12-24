package kr.hhplus.be.server.queue.port;

import java.util.Optional;
import kr.hhplus.be.server.queue.domain.ReservationToken;

public interface ReservationTokenRepository {
    Optional<ReservationToken> findLatestByUserId(String userId);

    Optional<ReservationToken> findByToken(String token);

    ReservationToken save(ReservationToken token);
}
