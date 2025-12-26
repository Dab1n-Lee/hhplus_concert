package kr.hhplus.be.server.lock.adapter.redis;

import kr.hhplus.be.server.lock.domain.DistributedLock;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class SimpleDistributedLock implements DistributedLock {
    private static final String LOCK_PREFIX = "lock:";
    private static final String LOCK_VALUE = "1";

    private final RedisTemplate<String, String> redisTemplate;

    public SimpleDistributedLock(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryLock(String key, long waitTimeMs, long leaseTimeMs) {
        String lockKey = LOCK_PREFIX + key;
        // SETNX with expiration: SET key value NX EX seconds
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, LOCK_VALUE, java.time.Duration.ofMillis(leaseTimeMs));
        return Boolean.TRUE.equals(acquired);
    }

    @Override
    public void unlock(String key) {
        String lockKey = LOCK_PREFIX + key;
        redisTemplate.delete(lockKey);
    }
}

