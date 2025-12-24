package kr.hhplus.be.server.reservation.controller;

import kr.hhplus.be.server.reservation.application.ReserveSeatCommand;
import kr.hhplus.be.server.reservation.application.ReserveSeatUseCase;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reservations")
public class ReservationController {
    private final ReserveSeatUseCase reserveSeatUseCase;

    public ReservationController(ReserveSeatUseCase reserveSeatUseCase) {
        this.reserveSeatUseCase = reserveSeatUseCase;
    }

    @PostMapping
    public ReservationResponse reserve(@RequestBody ReservationRequest request) {
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
