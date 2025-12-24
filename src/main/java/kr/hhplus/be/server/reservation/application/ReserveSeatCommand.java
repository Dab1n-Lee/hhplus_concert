package kr.hhplus.be.server.reservation.application;

import java.time.LocalDate;

public class ReserveSeatCommand {
    private final String userId;
    private final LocalDate concertDate;
    private final int seatNumber;

    public ReserveSeatCommand(String userId, LocalDate concertDate, int seatNumber) {
        this.userId = userId;
        this.concertDate = concertDate;
        this.seatNumber = seatNumber;
    }

    public String getUserId() {
        return userId;
    }

    public LocalDate getConcertDate() {
        return concertDate;
    }

    public int getSeatNumber() {
        return seatNumber;
    }
}
