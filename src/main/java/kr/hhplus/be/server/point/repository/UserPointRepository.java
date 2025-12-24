package kr.hhplus.be.server.point.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import kr.hhplus.be.server.point.domain.UserPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserPointRepository extends JpaRepository<UserPoint, Long> {
    Optional<UserPoint> findByUserId(String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserPoint u where u.userId = :userId")
    Optional<UserPoint> findByUserIdForUpdate(@Param("userId") String userId);
}
