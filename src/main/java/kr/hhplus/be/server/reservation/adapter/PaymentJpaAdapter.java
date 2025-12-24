package kr.hhplus.be.server.reservation.adapter;

import kr.hhplus.be.server.reservation.adapter.jpa.PaymentEntity;
import kr.hhplus.be.server.reservation.adapter.jpa.PaymentJpaRepository;
import kr.hhplus.be.server.reservation.domain.Payment;
import kr.hhplus.be.server.reservation.port.PaymentPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentJpaAdapter implements PaymentPort {
    private final PaymentJpaRepository paymentJpaRepository;

    public PaymentJpaAdapter(PaymentJpaRepository paymentJpaRepository) {
        this.paymentJpaRepository = paymentJpaRepository;
    }

    @Override
    @Transactional
    public Payment save(Payment payment) {
        PaymentEntity entity = new PaymentEntity(
            payment.getReservationId(),
            payment.getUserId(),
            payment.getAmount(),
            payment.getPaidAt()
        );
        PaymentEntity saved = paymentJpaRepository.save(entity);
        payment.setId(saved.getId());
        return payment;
    }
}
