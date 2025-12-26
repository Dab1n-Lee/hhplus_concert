package kr.hhplus.be.server.concert.dto;

import java.time.LocalDate;

public class ConcertRankingResponse {
    private LocalDate concertDate;
    private Long soldOutCount;
    private Long rank;

    public ConcertRankingResponse() {
    }

    public ConcertRankingResponse(LocalDate concertDate, Long soldOutCount, Long rank) {
        this.concertDate = concertDate;
        this.soldOutCount = soldOutCount;
        this.rank = rank;
    }

    public LocalDate getConcertDate() {
        return concertDate;
    }

    public void setConcertDate(LocalDate concertDate) {
        this.concertDate = concertDate;
    }

    public Long getSoldOutCount() {
        return soldOutCount;
    }

    public void setSoldOutCount(Long soldOutCount) {
        this.soldOutCount = soldOutCount;
    }

    public Long getRank() {
        return rank;
    }

    public void setRank(Long rank) {
        this.rank = rank;
    }
}
