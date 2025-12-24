package kr.hhplus.be.server.queue.controller;

public class ReservationTokenRequest {
    private String userId;

    public ReservationTokenRequest() {
    }

    public ReservationTokenRequest(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}
