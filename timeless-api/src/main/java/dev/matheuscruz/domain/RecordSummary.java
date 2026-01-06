package dev.matheuscruz.domain;

import java.math.BigDecimal;
import java.util.List;

public record RecordSummary(List<Record> records, Long totalRecords, BigDecimal totalExpenses, BigDecimal totalIncome) {
}
