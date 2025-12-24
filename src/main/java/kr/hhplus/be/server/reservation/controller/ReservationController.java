package kr.hhplus.be.server.reservation.controller;

import kr.hhplus.be.server.queue.application.ReservationTokenService;
import kr.hhplus.be.server.reservation.application.ReserveSeatCommand;
import kr.hhplus.be.server.reservation.application.ReserveSeatUseCase;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reservations")
public class ReservationController {
    private final ReserveSeatUseCase reserveSeatUseCase;
    private final ReservationTokenService reservationTokenService;

    public ReservationController(
        ReserveSeatUseCase reserveSeatUseCase,
        ReservationTokenService reservationTokenService
    ) {
        this.reserveSeatUseCase = reserveSeatUseCase;
        this.reservationTokenService = reservationTokenService;
    }

    @PostMapping
    public ReservationResponse reserve(
        @RequestHeader("Queue-Token") String queueToken,
        @RequestBody ReservationRequest request
    ) {
        reservationTokenService.validateActive(queueToken);
        var reservation = reserveSeatUseCase.reserve(
            new ReserveSeatCommand(request.getUserId(), request.getConcertDate(), request.getSeatNumber())
        );

        return new ReservationResponse(
            reservation.getId(),
            request.getSeatNumber(),
            reservation.getExpiresAt()
        );
    }
}
