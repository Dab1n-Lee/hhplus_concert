package kr.hhplus.be.server.point.dto;

public class PointBalanceResponse {
    private final String userId;
    private final long balance;

    public PointBalanceResponse(String userId, long balance) {
        this.userId = userId;
        this.balance = balance;
    }

    public String getUserId() {
        return userId;
    }

    public long getBalance() {
        return balance;
    }
}
