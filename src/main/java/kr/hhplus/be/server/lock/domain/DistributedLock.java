package kr.hhplus.be.server.lock.domain;

import java.util.function.Supplier;

public interface DistributedLock {
    /**
     * Try to acquire lock. Returns true if lock is acquired, false otherwise.
     *
     * @param key lock key
     * @param waitTimeMs maximum time to wait for lock acquisition in milliseconds
     * @param leaseTimeMs lock lease time in milliseconds (how long the lock will be held)
     * @return true if lock acquired, false otherwise
     */
    boolean tryLock(String key, long waitTimeMs, long leaseTimeMs);

    /**
     * Release the lock.
     *
     * @param key lock key
     */
    void unlock(String key);

    /**
     * Execute a supplier function with lock protection.
     * Lock is automatically released after execution.
     *
     * @param key lock key
     * @param waitTimeMs maximum time to wait for lock acquisition in milliseconds
     * @param leaseTimeMs lock lease time in milliseconds
     * @param supplier function to execute
     * @param <T> return type
     * @return result of supplier function
     * @throws LockAcquisitionFailedException if lock cannot be acquired
     */
    default <T> T executeWithLock(String key, long waitTimeMs, long leaseTimeMs, Supplier<T> supplier) {
        if (!tryLock(key, waitTimeMs, leaseTimeMs)) {
            throw new LockAcquisitionFailedException("Failed to acquire lock for key: " + key);
        }
        try {
            return supplier.get();
        } finally {
            unlock(key);
        }
    }

    /**
     * Execute a runnable function with lock protection.
     * Lock is automatically released after execution.
     *
     * @param key lock key
     * @param waitTimeMs maximum time to wait for lock acquisition in milliseconds
     * @param leaseTimeMs lock lease time in milliseconds
     * @param runnable function to execute
     * @throws LockAcquisitionFailedException if lock cannot be acquired
     */
    default void executeWithLock(String key, long waitTimeMs, long leaseTimeMs, Runnable runnable) {
        if (!tryLock(key, waitTimeMs, leaseTimeMs)) {
            throw new LockAcquisitionFailedException("Failed to acquire lock for key: " + key);
        }
        try {
            runnable.run();
        } finally {
            unlock(key);
        }
    }
}

