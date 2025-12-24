package kr.hhplus.be.server.reservation.port;

import java.time.LocalDateTime;

public interface ClockProvider {
    LocalDateTime now();
}
