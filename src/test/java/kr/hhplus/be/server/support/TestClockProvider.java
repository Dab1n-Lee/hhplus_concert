package kr.hhplus.be.server.support;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;
import kr.hhplus.be.server.reservation.port.ClockProvider;

public class TestClockProvider implements ClockProvider {
    private final AtomicReference<LocalDateTime> now =
        new AtomicReference<>(LocalDateTime.of(2025, 1, 1, 0, 0));

    public void setNow(LocalDateTime now) {
        this.now.set(now);
    }

    @Override
    public LocalDateTime now() {
        return now.get();
    }
}
