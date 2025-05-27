package dev.matheuscruz.infra.persistence.projection;

import dev.matheuscruz.domain.RecordType;
import java.math.BigDecimal;

public class AmountTypeProjection {

    private final BigDecimal amount;
    private final RecordType recordType;

    public AmountTypeProjection(BigDecimal amount, RecordType recordType) {
        this.amount = amount;
        this.recordType = recordType;
    }

    public RecordType getRecordType() {
        return recordType;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
