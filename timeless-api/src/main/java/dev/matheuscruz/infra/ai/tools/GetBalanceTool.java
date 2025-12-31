package dev.matheuscruz.infra.ai.tools;

import dev.langchain4j.agent.tool.Tool;
import dev.matheuscruz.domain.AmountAndTypeOnly;
import dev.matheuscruz.domain.RecordRepository;
import dev.matheuscruz.domain.Transactions;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.util.List;

@ApplicationScoped
public class GetBalanceTool {

    final RecordRepository recordRepository;

    public GetBalanceTool(RecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

    @Tool(value = "get account balance for a user")
    public BigDecimal getBalance(String userId) {
        List<AmountAndTypeOnly> list = this.recordRepository.getRecordsWithAmountAndTypeOnlyByUser(userId);
        return list.stream()
                .map(record -> record.getTransaction().equals(Transactions.OUT)
                        ? record.getAmount().multiply(new BigDecimal(-1))
                        : record.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

    }
}
