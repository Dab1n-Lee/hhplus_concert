package kr.hhplus.be.server.lock.domain;

public class LockAcquisitionFailedException extends RuntimeException {
    public LockAcquisitionFailedException(String message) {
        super(message);
    }
}

