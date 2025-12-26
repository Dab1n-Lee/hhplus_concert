package kr.hhplus.be.server.lock.adapter.redis;

import kr.hhplus.be.server.lock.domain.DistributedLock;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class SpinDistributedLock implements DistributedLock {
    private static final String LOCK_PREFIX = "lock:";
    private static final String LOCK_VALUE = "1";
    private static final long DEFAULT_SPIN_INTERVAL_MS = 10; // 10ms between retries

    private final RedisTemplate<String, String> redisTemplate;

    public SpinDistributedLock(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryLock(String key, long waitTimeMs, long leaseTimeMs) {
        String lockKey = LOCK_PREFIX + key;
        long startTime = System.currentTimeMillis();
        long maxRetries = waitTimeMs / DEFAULT_SPIN_INTERVAL_MS;
        long retryCount = 0;

        while (retryCount < maxRetries) {
            Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, LOCK_VALUE, java.time.Duration.ofMillis(leaseTimeMs));
            
            if (Boolean.TRUE.equals(acquired)) {
                return true;
            }

            // Check if wait time has elapsed
            if (System.currentTimeMillis() - startTime >= waitTimeMs) {
                return false;
            }

            // Sleep before retry
            try {
                Thread.sleep(DEFAULT_SPIN_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }

            retryCount++;
        }

        return false;
    }

    @Override
    public void unlock(String key) {
        String lockKey = LOCK_PREFIX + key;
        redisTemplate.delete(lockKey);
    }
}

