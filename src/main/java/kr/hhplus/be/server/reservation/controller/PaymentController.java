package kr.hhplus.be.server.reservation.controller;

import kr.hhplus.be.server.queue.application.ReservationTokenService;
import kr.hhplus.be.server.reservation.application.PayReservationCommand;
import kr.hhplus.be.server.reservation.application.PayReservationUseCase;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentController {
    private final PayReservationUseCase payReservationUseCase;
    private final ReservationTokenService reservationTokenService;

    public PaymentController(
        PayReservationUseCase payReservationUseCase,
        ReservationTokenService reservationTokenService
    ) {
        this.payReservationUseCase = payReservationUseCase;
        this.reservationTokenService = reservationTokenService;
    }

    @PostMapping
    public PaymentResponse pay(
        @RequestHeader("Queue-Token") String queueToken,
        @RequestBody PaymentRequest request
    ) {
        reservationTokenService.validateActive(queueToken);
        var payment = payReservationUseCase.pay(
            new PayReservationCommand(request.getReservationId(), request.getUserId(), request.getAmount())
        );
        reservationTokenService.complete(queueToken);
        return new PaymentResponse(
            payment.getId(),
            payment.getReservationId(),
            payment.getAmount(),
            payment.getPaidAt()
        );
    }
}
