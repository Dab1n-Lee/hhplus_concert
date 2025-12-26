package kr.hhplus.be.server.reservation.application.event;

import kr.hhplus.be.server.reservation.application.dataplatform.DataPlatformSendService;
import kr.hhplus.be.server.reservation.application.dataplatform.ReservationDataPlatformPayload;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ReservationCompletedEventListener {
    private final DataPlatformSendService dataPlatformSendService;

    public ReservationCompletedEventListener(DataPlatformSendService dataPlatformSendService) {
        this.dataPlatformSendService = dataPlatformSendService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReservationCompleted(ReservationCompletedEvent event) {
        ReservationDataPlatformPayload payload = new ReservationDataPlatformPayload(event);
        dataPlatformSendService.send(payload);
    }
}
