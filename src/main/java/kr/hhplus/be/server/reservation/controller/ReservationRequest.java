package kr.hhplus.be.server.reservation.controller;

import java.time.LocalDate;

public class ReservationRequest {
    private String userId;
    private LocalDate concertDate;
    private int seatNumber;

    public ReservationRequest() {
    }

    public ReservationRequest(String userId, LocalDate concertDate, int seatNumber) {
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
