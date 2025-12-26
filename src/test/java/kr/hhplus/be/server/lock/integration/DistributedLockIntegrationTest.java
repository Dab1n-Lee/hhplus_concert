package kr.hhplus.be.server.lock.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import kr.hhplus.be.server.TestcontainersConfiguration;
import kr.hhplus.be.server.lock.adapter.redis.SimpleDistributedLock;
import kr.hhplus.be.server.lock.adapter.redis.SpinDistributedLock;
import kr.hhplus.be.server.lock.domain.LockAcquisitionFailedException;
import kr.hhplus.be.server.point.domain.UserPoint;
import kr.hhplus.be.server.point.repository.UserPointRepository;
import kr.hhplus.be.server.point.service.PointService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class})
class DistributedLockIntegrationTest {

    @Autowired
    private SimpleDistributedLock simpleDistributedLock;

    @Autowired
    private SpinDistributedLock spinDistributedLock;

    @Autowired
    private PointService pointService;

    @Autowired
    private UserPointRepository userPointRepository;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    private static final String TEST_USER_ID = "user-lock-test";
    private static final long INITIAL_BALANCE = 1000L;

    @BeforeEach
    void setUp() {
        userPointRepository.save(new UserPoint(TEST_USER_ID, INITIAL_BALANCE));
        // Clear Redis locks
        redisConnectionFactory.getConnection().flushAll();
    }

    @AfterEach
    void tearDown() {
        userPointRepository.deleteAll();
        // Clear Redis locks
        redisConnectionFactory.getConnection().flushAll();
    }

    @Test
    void simpleLock_preventsConcurrentAccess() throws Exception {
        String lockKey = "test:simple:lock";
        AtomicInteger counter = new AtomicInteger(0);
        int threadCount = 10;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            results.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    simpleDistributedLock.executeWithLock(
                        lockKey,
                        100, // waitTimeMs
                        1000, // leaseTimeMs
                        () -> {
                            int current = counter.get();
                            Thread.sleep(50); // Simulate work
                            counter.set(current + 1);
                            return true;
                        }
                    );
                    return true;
                } catch (LockAcquisitionFailedException e) {
                    return false;
                } catch (Exception e) {
                    return false;
                }
            }));
        }

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();

        int successCount = 0;
        for (Future<Boolean> result : results) {
            if (result.get(10, TimeUnit.SECONDS)) {
                successCount++;
            }
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Only one thread should succeed with Simple Lock (no retry)
        assertThat(successCount).isEqualTo(1);
        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    void spinLock_allowsRetryAndPreventsConcurrentAccess() throws Exception {
        String lockKey = "test:spin:lock";
        AtomicInteger counter = new AtomicInteger(0);
        int threadCount = 5;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            results.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    spinDistributedLock.executeWithLock(
                        lockKey,
                        2000, // waitTimeMs - allow retry
                        100, // leaseTimeMs - short lease
                        () -> {
                            int current = counter.get();
                            Thread.sleep(100); // Simulate work
                            counter.set(current + 1);
                            return true;
                        }
                    );
                    return true;
                } catch (LockAcquisitionFailedException e) {
                    return false;
                } catch (Exception e) {
                    return false;
                }
            }));
        }

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();

        int successCount = 0;
        for (Future<Boolean> result : results) {
            if (result.get(10, TimeUnit.SECONDS)) {
                successCount++;
            }
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // All threads should eventually succeed with Spin Lock (with retry)
        assertThat(successCount).isEqualTo(threadCount);
        assertThat(counter.get()).isEqualTo(threadCount);
    }

    @Test
    void distributedLock_preventsConcurrentPointCharge() throws Exception {
        int threadCount = 10;
        long chargeAmount = 100L;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<UserPoint>> results = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            results.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                return pointService.charge(TEST_USER_ID, chargeAmount);
            }));
        }

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();

        List<UserPoint> userPoints = new ArrayList<>();
        for (Future<UserPoint> result : results) {
            userPoints.add(result.get(10, TimeUnit.SECONDS));
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // All charges should succeed
        assertThat(userPoints).hasSize(threadCount);

        // Final balance should be correct
        UserPoint finalUserPoint = userPointRepository.findByUserId(TEST_USER_ID)
            .orElseThrow();
        long expectedBalance = INITIAL_BALANCE + (chargeAmount * threadCount);
        assertThat(finalUserPoint.getBalance()).isEqualTo(expectedBalance);
    }

    @Test
    void distributedLock_preventsConcurrentPointUse() throws Exception {
        // Charge first to have enough balance
        pointService.charge(TEST_USER_ID, 5000L);

        int threadCount = 10;
        long useAmount = 100L;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<UserPoint>> results = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            results.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    return pointService.use(TEST_USER_ID, useAmount);
                } catch (Exception e) {
                    return null;
                }
            }));
        }

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();

        int successCount = 0;
        for (Future<UserPoint> result : results) {
            UserPoint userPoint = result.get(10, TimeUnit.SECONDS);
            if (userPoint != null) {
                successCount++;
            }
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // All uses should succeed (balance is sufficient)
        assertThat(successCount).isEqualTo(threadCount);

        // Final balance should be correct
        UserPoint finalUserPoint = userPointRepository.findByUserId(TEST_USER_ID)
            .orElseThrow();
        long expectedBalance = INITIAL_BALANCE + 5000L - (useAmount * threadCount);
        assertThat(finalUserPoint.getBalance()).isEqualTo(expectedBalance);
        assertThat(finalUserPoint.getBalance()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void distributedLock_throwsExceptionWhenLockCannotBeAcquired() {
        String lockKey = "test:lock:timeout";

        // Acquire lock first
        boolean acquired = simpleDistributedLock.tryLock(lockKey, 100, 5000);
        assertThat(acquired).isTrue();

        // Try to acquire same lock with short wait time - should fail
        assertThatThrownBy(() -> {
            simpleDistributedLock.executeWithLock(
                lockKey,
                100, // short wait time
                1000,
                () -> "should not execute"
            );
        }).isInstanceOf(LockAcquisitionFailedException.class);

        // Release lock
        simpleDistributedLock.unlock(lockKey);
    }
}

