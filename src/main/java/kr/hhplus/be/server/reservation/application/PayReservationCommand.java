package kr.hhplus.be.server.reservation.application;

public class PayReservationCommand {
    private final Long reservationId;
    private final String userId;
    private final long amount;

    public PayReservationCommand(Long reservationId, String userId, long amount) {
        this.reservationId = reservationId;
        this.userId = userId;
        this.amount = amount;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public String getUserId() {
        return userId;
    }

    public long getAmount() {
        return amount;
    }
}
