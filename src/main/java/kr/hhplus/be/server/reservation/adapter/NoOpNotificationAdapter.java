package kr.hhplus.be.server.reservation.adapter;

import kr.hhplus.be.server.reservation.port.NotificationPort;
import org.springframework.stereotype.Component;

@Component
public class NoOpNotificationAdapter implements NotificationPort {
    @Override
    public void sendReservationConfirmed(Long reservationId, String userId, int seatNumber) {
        // no-op for now
    }
}
