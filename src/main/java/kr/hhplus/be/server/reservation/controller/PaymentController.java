package kr.hhplus.be.server.reservation.controller;

import kr.hhplus.be.server.reservation.application.PayReservationCommand;
import kr.hhplus.be.server.reservation.application.PayReservationUseCase;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentController {
    private final PayReservationUseCase payReservationUseCase;

    public PaymentController(PayReservationUseCase payReservationUseCase) {
        this.payReservationUseCase = payReservationUseCase;
    }

    @PostMapping
    public PaymentResponse pay(@RequestBody PaymentRequest request) {
        var payment = payReservationUseCase.pay(
            new PayReservationCommand(request.getReservationId(), request.getUserId(), request.getAmount())
        );
        return new PaymentResponse(
            payment.getId(),
            payment.getReservationId(),
            payment.getAmount(),
            payment.getPaidAt()
        );
    }
}
