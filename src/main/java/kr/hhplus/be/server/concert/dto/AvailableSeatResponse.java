package kr.hhplus.be.server.concert.dto;

public class AvailableSeatResponse {
    private final int seatNumber;

    public AvailableSeatResponse(int seatNumber) {
        this.seatNumber = seatNumber;
    }

    public int getSeatNumber() {
        return seatNumber;
    }
}
