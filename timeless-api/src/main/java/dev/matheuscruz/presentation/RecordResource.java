package dev.matheuscruz.presentation;

import dev.matheuscruz.domain.OutcomeType;
import dev.matheuscruz.domain.Record;
import dev.matheuscruz.domain.RecordType;
import dev.matheuscruz.domain.User;
import dev.matheuscruz.infra.persistence.RecordRepository;
import dev.matheuscruz.infra.persistence.UserRepository;
import dev.matheuscruz.infra.security.AESAdapter;
import io.quarkus.narayana.jta.QuarkusTransaction;
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Path("/api/records")
public class RecordResource {

    RecordRepository recordRepository;
    UserRepository userRepository;
    AESAdapter aesAdapter;
    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public RecordResource(
            RecordRepository recordRepository,
            UserRepository userRepository,
            AESAdapter aesAdapter
    ) {
        this.recordRepository = recordRepository;
        this.userRepository = userRepository;
        this.aesAdapter = aesAdapter;
    }

    @POST
    public Response createRecord(@Valid CreateRecordRequest req) {

        String phoneNumber = this.aesAdapter.tryEncrypt(req.from());
        User user = this.userRepository.find("phoneNumber = :phoneNumber", Parameters.with("phoneNumber", phoneNumber))
                .firstResultOptional()
                .orElseThrow(ForbiddenException::new);

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
    public Response getRecords() {
        List<RecordItem> output = recordRepository.findAll()
                .list()
                .stream().map(record -> {
                    String format = record.getCreatedAt().atZone(ZoneId.of("America/Sao_Paulo")).toLocalDate().format(formatter);
                    return new RecordItem(record.getId(), record.getAmount(), record.getDescription(), record.getRecordType().name(), format);
                }).toList();
        return Response.ok(output).build();
    }

    public record RecordItem(Long id, BigDecimal amount, String description, String recordType, String createdAt) {
    }

    public record CreateRecordRequest(
            @PositiveOrZero
            BigDecimal amount,
            @NotBlank
            String description,
            @NotNull
            RecordType recordType,
            @NotBlank
            String from) {
    }

}
