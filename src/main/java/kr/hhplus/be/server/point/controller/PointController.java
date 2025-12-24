package kr.hhplus.be.server.point.controller;

import kr.hhplus.be.server.point.dto.PointBalanceResponse;
import kr.hhplus.be.server.point.dto.PointChargeRequest;
import kr.hhplus.be.server.point.service.PointService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/points")
public class PointController {
    private final PointService pointService;

    public PointController(PointService pointService) {
        this.pointService = pointService;
    }

    @PostMapping("/charge")
    public PointBalanceResponse charge(@RequestBody PointChargeRequest request) {
        var updated = pointService.charge(request.getUserId(), request.getAmount());
        return new PointBalanceResponse(updated.getUserId(), updated.getBalance());
    }

    @GetMapping("/{userId}")
    public PointBalanceResponse getPoint(@PathVariable String userId) {
        var userPoint = pointService.getPoint(userId);
        return new PointBalanceResponse(userPoint.getUserId(), userPoint.getBalance());
    }
}
