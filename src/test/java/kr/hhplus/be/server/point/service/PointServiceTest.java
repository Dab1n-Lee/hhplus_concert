package kr.hhplus.be.server.point.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import kr.hhplus.be.server.point.domain.UserPoint;
import kr.hhplus.be.server.point.repository.UserPointRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {
    @Mock
    private UserPointRepository userPointRepository;

    @InjectMocks
    private PointService pointService;

    @Test
    void chargesPointToNewUser() {
        String userId = "user-1";
        when(userPointRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.empty());
        when(userPointRepository.save(any(UserPoint.class))).thenReturn(new UserPoint(userId, 0L));

        UserPoint updated = pointService.charge(userId, 100L);

        ArgumentCaptor<UserPoint> captor = ArgumentCaptor.forClass(UserPoint.class);
        verify(userPointRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(updated.getBalance()).isEqualTo(100L);
    }

    @Test
    void chargesPointToExistingUser() {
        String userId = "user-2";
        UserPoint existing = new UserPoint(userId, 50L);
        when(userPointRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(existing));

        UserPoint updated = pointService.charge(userId, 70L);

        assertThat(updated.getBalance()).isEqualTo(120L);
    }

    @Test
    void rejectsNonPositiveCharge() {
        assertThatThrownBy(() -> pointService.charge("user-3", 0L))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void returnsZeroWhenUserNotFound() {
        when(userPointRepository.findByUserId("user-4")).thenReturn(Optional.empty());

        UserPoint point = pointService.getPoint("user-4");

        assertThat(point.getBalance()).isZero();
    }
}
