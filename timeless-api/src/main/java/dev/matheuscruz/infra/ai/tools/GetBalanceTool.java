package dev.matheuscruz.infra.ai.tools;

import dev.langchain4j.agent.tool.Tool;
import dev.matheuscruz.domain.RecordType;
import dev.matheuscruz.infra.persistence.RecordRepository;
import dev.matheuscruz.infra.persistence.projection.AmountTypeProjection;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.util.List;

@ApplicationScoped
public class GetBalanceTool {

    final RecordRepository recordRepository;

    public GetBalanceTool(RecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

    @Tool(value = "get account balance")
    public BigDecimal getBalance() {
        List<AmountTypeProjection> list = this.recordRepository.getRecordsWithAmountAndTypeOnly();
        return list.stream()
                .map(record -> record.getRecordType().equals(RecordType.OUT)
                        ? record.getAmount().multiply(new BigDecimal(-1))
                        : record.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

    }
}
