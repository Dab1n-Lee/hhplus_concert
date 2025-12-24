package kr.hhplus.be.server.reservation.port;

import kr.hhplus.be.server.reservation.domain.Payment;

public interface PaymentRepository {
    Payment save(Payment payment);
}
