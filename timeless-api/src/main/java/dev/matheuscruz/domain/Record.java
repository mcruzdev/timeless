package dev.matheuscruz.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "records")
public class Record {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String userId;
    private BigDecimal amount;
    private String description;
    @Enumerated(EnumType.STRING)
    private RecordType recordType;
    @Enumerated(EnumType.STRING)
    private OutcomeType outcomeType;

    protected Record() {
    }

    private Record(String userId, BigDecimal amount, String description, RecordType recordType, OutcomeType outcomeType) {
        this.userId = userId;
        this.amount = amount;
        this.description = description;
        this.recordType = recordType;
        this.outcomeType = outcomeType;
    }

    public static Record createIncome(String userId, BigDecimal amount, String description) {
        return new Record(userId, amount, description, RecordType.IN, OutcomeType.NONE);
    }

    public static Record createOutcome(String userId, BigDecimal amount, String description, OutcomeType type) {
        return new Record(
                Objects.requireNonNull(userId),
                Objects.requireNonNull(amount),
                Objects.requireNonNull(description),
                Objects.requireNonNull(RecordType.OUT),
                Objects.requireNonNullElse(type, OutcomeType.GENERAL));
    }

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public RecordType getRecordType() {
        return recordType;
    }

    public OutcomeType getOutcomeType() {
        return outcomeType;
    }
}
