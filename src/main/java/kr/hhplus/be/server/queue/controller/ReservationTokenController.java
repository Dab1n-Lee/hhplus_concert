package kr.hhplus.be.server.queue.controller;

import kr.hhplus.be.server.queue.application.ReservationTokenService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/queue/tokens")
public class ReservationTokenController {
    private final ReservationTokenService reservationTokenService;

    public ReservationTokenController(ReservationTokenService reservationTokenService) {
        this.reservationTokenService = reservationTokenService;
    }

    @PostMapping
    public ReservationTokenResponse issue(@RequestBody ReservationTokenRequest request) {
        var token = reservationTokenService.issue(request.getUserId());
        return new ReservationTokenResponse(
            token.getToken(),
            token.getStatus(),
            token.getPosition(),
            token.getExpiresAt()
        );
    }
}
