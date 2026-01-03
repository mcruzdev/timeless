package dev.matheuscruz.presentation.data;

import java.math.BigDecimal;
import java.util.List;

public record PageRecord(List<RecordItemResponse> items, Long totalRecords, BigDecimal totalExpenses,
        BigDecimal totalIn) {
}
