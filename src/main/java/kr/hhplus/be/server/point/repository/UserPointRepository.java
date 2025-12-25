package kr.hhplus.be.server.point.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import kr.hhplus.be.server.point.domain.UserPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserPointRepository extends JpaRepository<UserPoint, Long> {
    Optional<UserPoint> findByUserId(String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserPoint u where u.userId = :userId")
    Optional<UserPoint> findByUserIdForUpdate(@Param("userId") String userId);

    /**
     * 잔액이 충분한 경우에만 차감하는 조건부 UPDATE
     * @param userId 사용자 ID
     * @param amount 차감할 금액
     * @return 업데이트된 행 수 (1이면 성공, 0이면 잔액 부족)
     */
    @Modifying
    @Transactional
    @Query("update UserPoint u set u.balance = u.balance - :amount where u.userId = :userId and u.balance >= :amount")
    int deductIfSufficient(@Param("userId") String userId, @Param("amount") long amount);
}
