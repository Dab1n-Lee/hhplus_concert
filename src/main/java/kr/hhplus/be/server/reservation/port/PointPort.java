package kr.hhplus.be.server.reservation.port;

public interface PointPort {
    long getBalance(String userId);

    void use(String userId, long amount);
}
