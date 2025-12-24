package kr.hhplus.be.server.reservation.port;

import kr.hhplus.be.server.reservation.domain.Payment;

public interface PaymentPort {
    Payment save(Payment payment);
}
