package kr.hhplus.be.server.point.dto;

public class PointChargeRequest {
    private String userId;
    private long amount;

    public PointChargeRequest() {
    }

    public PointChargeRequest(String userId, long amount) {
        this.userId = userId;
        this.amount = amount;
    }

    public String getUserId() {
        return userId;
    }

    public long getAmount() {
        return amount;
    }
}
