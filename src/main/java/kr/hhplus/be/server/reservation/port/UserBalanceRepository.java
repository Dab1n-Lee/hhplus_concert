package kr.hhplus.be.server.reservation.port;

public interface UserBalanceRepository {
    long getBalance(String userId);

    void use(String userId, long amount);
}
