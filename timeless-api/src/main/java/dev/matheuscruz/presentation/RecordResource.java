package dev.matheuscruz.presentation;

import dev.matheuscruz.domain.OutcomeType;
import dev.matheuscruz.domain.Record;
import dev.matheuscruz.domain.RecordType;
import dev.matheuscruz.domain.User;
import dev.matheuscruz.infra.persistence.RecordRepository;
import dev.matheuscruz.infra.persistence.UserRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.jboss.resteasy.reactive.RestQuery;

@Path("/api/records")
public class RecordResource {

    RecordRepository recordRepository;
    UserRepository userRepository;
    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static Instant INSTANT_2025 = LocalDateTime.of(2025, 1, 1, 0, 0, 0).toInstant(ZoneOffset.UTC);

    public RecordResource(RecordRepository recordRepository, UserRepository userRepository) {
        this.recordRepository = recordRepository;
        this.userRepository = userRepository;
    }

    @POST
    public Response createRecord(@Valid CreateRecordRequest req) {

        User user = this.userRepository.find("phoneNumber = :phoneNumber", Parameters.with("phoneNumber", req.from()))
                .firstResultOptional().orElseThrow(ForbiddenException::new);

        QuarkusTransaction.begin();
        Record record;
        if (req.recordType().equals(RecordType.OUT)) {
            record = Record.createOutcome(user.getId(), req.amount(), req.description(), OutcomeType.GENERAL);
            this.recordRepository.persist(record);
        } else {
            record = Record.createIncome(user.getId(), req.amount(), req.description());
            this.recordRepository.persist(record);
        }
        QuarkusTransaction.commit();

        return Response.created(URI.create("/api/records/" + record.getId())).build();
    }

    @GET
    public Response getRecords(@RestQuery("page") String p, @RestQuery("limit") String l) {

        int page = Integer.parseInt(Optional.of(p).orElse("0"));
        int limit = Integer.parseInt(Optional.of(l).orElse("10"));

        long totalRecords = recordRepository.count();

        List<RecordItem> output = recordRepository.findAll().page(Page.of(page, limit)).list().stream().map(record -> {
            String format = record.getCreatedAt().atZone(ZoneId.of("America/Sao_Paulo")).toLocalDate()
                    .format(formatter);
            return new RecordItem(record.getId(), record.getAmount(), record.getDescription(),
                    record.getRecordType().name(), format);
        }).toList();

        List<Record> list = recordRepository.find("createdAt >= :instant AND createdAt <= :now",
                Parameters.with("instant", INSTANT_2025).and("now", Instant.now())).list();

        Optional<BigDecimal> totalExpenses = list.stream().filter(item -> item.getRecordType().equals(RecordType.OUT))
                .map(Record::getAmount).reduce(BigDecimal::add);

        Optional<BigDecimal> totalIn = list.stream().filter(item -> item.getRecordType().equals(RecordType.IN))
                .map(Record::getAmount).reduce(BigDecimal::add);

        return Response.ok(new PageRecord(output, totalRecords, totalExpenses.orElse(BigDecimal.ZERO),
                totalIn.orElse(BigDecimal.ZERO))).build();
    }

    public record PageRecord(List<RecordItem> items, Long totalRecords, BigDecimal totalExpenses, BigDecimal totalIn) {
    }

    public record RecordItem(Long id, BigDecimal amount, String description, String recordType, String createdAt) {
    }

    public record CreateRecordRequest(@PositiveOrZero BigDecimal amount, @NotBlank String description,
            @NotNull RecordType recordType, @NotBlank String from) {
    }

}
