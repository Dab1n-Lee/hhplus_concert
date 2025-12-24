package kr.hhplus.be.server.reservation.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import kr.hhplus.be.server.TestcontainersConfiguration;
import kr.hhplus.be.server.concert.domain.SeatStatus;
import kr.hhplus.be.server.concert.repository.ConcertDateRepository;
import kr.hhplus.be.server.concert.repository.SeatRepository;
import kr.hhplus.be.server.point.repository.UserPointRepository;
import kr.hhplus.be.server.queue.adapter.jpa.ReservationTokenJpaRepository;
import kr.hhplus.be.server.queue.domain.ReservationTokenStatus;
import kr.hhplus.be.server.reservation.adapter.jpa.PaymentJpaRepository;
import kr.hhplus.be.server.reservation.adapter.jpa.ReservationJpaRepository;
import kr.hhplus.be.server.reservation.domain.ReservationStatus;
import kr.hhplus.be.server.support.TestClockConfiguration;
import kr.hhplus.be.server.support.TestClockProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestClockConfiguration.class})
class ReservationFlowIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestClockProvider testClockProvider;

    @Autowired
    private ReservationJpaRepository reservationJpaRepository;

    @Autowired
    private PaymentJpaRepository paymentJpaRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ConcertDateRepository concertDateRepository;

    @Autowired
    private UserPointRepository userPointRepository;

    @Autowired
    private ReservationTokenJpaRepository reservationTokenJpaRepository;

    @AfterEach
    void tearDown() {
        paymentJpaRepository.deleteAll();
        reservationJpaRepository.deleteAll();
        seatRepository.deleteAll();
        concertDateRepository.deleteAll();
        userPointRepository.deleteAll();
        reservationTokenJpaRepository.deleteAll();
    }

    @Test
    void issuesTokenReservesSeatAndPays() throws Exception {
        LocalDateTime now = LocalDateTime.of(2025, 1, 1, 10, 0);
        testClockProvider.setNow(now);

        String token = issueToken("user-1");
        chargePoints("user-1", 10_000L);

        Long reservationId = reserveSeat(token, "user-1", LocalDate.of(2025, 1, 1), 10);
        payReservation(token, reservationId, "user-1", 5_000L);

        var reservation = reservationJpaRepository.findById(reservationId).orElseThrow();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);

        var seat = seatRepository.findById(reservation.getSeatId()).orElseThrow();
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.RESERVED);
        assertThat(seat.getReservedUserId()).isEqualTo("user-1");

        var tokenEntity = reservationTokenJpaRepository.findByToken(token).orElseThrow();
        assertThat(tokenEntity.getStatus()).isEqualTo(ReservationTokenStatus.DONE);
    }

    @Test
    void allowsReservationAfterHoldExpires() throws Exception {
        LocalDate concertDate = LocalDate.of(2025, 1, 1);
        LocalDateTime holdTime = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime afterExpiry = holdTime.plusMinutes(6);

        testClockProvider.setNow(holdTime);
        String token1 = issueToken("user-1");
        Long firstReservationId = reserveSeat(token1, "user-1", concertDate, 10);

        testClockProvider.setNow(afterExpiry);
        String token2 = issueToken("user-2");
        Long secondReservationId = reserveSeat(token2, "user-2", concertDate, 10);

        var firstReservation = reservationJpaRepository.findById(firstReservationId).orElseThrow();
        assertThat(firstReservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);

        var secondReservation = reservationJpaRepository.findById(secondReservationId).orElseThrow();
        var seat = seatRepository.findById(secondReservation.getSeatId()).orElseThrow();
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.HELD);
        assertThat(seat.getHoldUserId()).isEqualTo("user-2");
    }

    private String issueToken(String userId) throws Exception {
        MvcResult result = mockMvc.perform(post("/queue/tokens")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of("userId", userId))))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("token").asText();
    }

    private void chargePoints(String userId, long amount) throws Exception {
        mockMvc.perform(post("/points/charge")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                    "userId", userId,
                    "amount", amount
                ))))
            .andExpect(status().isOk());
    }

    private Long reserveSeat(String token, String userId, LocalDate date, int seatNumber) throws Exception {
        MvcResult result = mockMvc.perform(post("/reservations")
                .header("Queue-Token", token)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                    "userId", userId,
                    "concertDate", date.toString(),
                    "seatNumber", seatNumber
                ))))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("reservationId").asLong();
    }

    private void payReservation(String token, Long reservationId, String userId, long amount) throws Exception {
        mockMvc.perform(post("/payments")
                .header("Queue-Token", token)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                    "reservationId", reservationId,
                    "userId", userId,
                    "amount", amount
                ))))
            .andExpect(status().isOk());
    }
}
