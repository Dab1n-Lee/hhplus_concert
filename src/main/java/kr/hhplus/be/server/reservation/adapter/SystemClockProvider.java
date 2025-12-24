package kr.hhplus.be.server.reservation.adapter;

import java.time.LocalDateTime;
import kr.hhplus.be.server.reservation.port.ClockProvider;
import org.springframework.stereotype.Component;

@Component
public class SystemClockProvider implements ClockProvider {
    @Override
    public LocalDateTime now() {
        return LocalDateTime.now();
    }
}
