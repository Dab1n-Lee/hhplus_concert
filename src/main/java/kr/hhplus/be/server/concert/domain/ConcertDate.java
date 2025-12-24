package kr.hhplus.be.server.concert.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "concert_date")
public class ConcertDate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "concert_date", nullable = false, unique = true)
    private LocalDate concertDate;

    protected ConcertDate() {
    }

    public ConcertDate(LocalDate concertDate) {
        this.concertDate = concertDate;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getConcertDate() {
        return concertDate;
    }
}
