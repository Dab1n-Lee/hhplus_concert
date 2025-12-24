package kr.hhplus.be.server.reservation.port;

public interface NotificationPort {
    void sendReservationConfirmed(Long reservationId, String userId, int seatNumber);
}
