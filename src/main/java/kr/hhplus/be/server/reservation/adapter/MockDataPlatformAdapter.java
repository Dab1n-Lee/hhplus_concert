package kr.hhplus.be.server.reservation.adapter;

import kr.hhplus.be.server.reservation.application.dataplatform.DataPlatformSendService;
import kr.hhplus.be.server.reservation.application.dataplatform.ReservationDataPlatformPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MockDataPlatformAdapter implements DataPlatformSendService {
    private static final Logger log = LoggerFactory.getLogger(MockDataPlatformAdapter.class);

    @Override
    public void send(ReservationDataPlatformPayload payload) {
        // Mock API 호출: 실제 데이터 플랫폼으로 예약 정보 전송
        // 실제 구현 시에는 HTTP 클라이언트를 사용하여 외부 API 호출
        log.info(
            "[Mock Data Platform] 예약 정보 전송 - reservationId: {}, userId: {}, paymentId: {}, amount: {}, concertDate: {}, seatNumber: {}",
            payload.getReservationId(),
            payload.getUserId(),
            payload.getPaymentId(),
            payload.getAmount(),
            payload.getConcertDate(),
            payload.getSeatNumber()
        );
    }
}
