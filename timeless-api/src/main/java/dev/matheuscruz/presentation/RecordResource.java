package dev.matheuscruz.presentation;

import java.math.BigDecimal;
import java.net.URI;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import dev.matheuscruz.domain.OutcomeType;
import dev.matheuscruz.domain.Record;
import dev.matheuscruz.domain.RecordType;
import dev.matheuscruz.domain.User;
import dev.matheuscruz.infra.persistence.RecordRepository;
import dev.matheuscruz.infra.persistence.UserRepository;
import io.quarkus.logging.Log;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestQuery;

@Path("/api/records")
public class RecordResource {

    RecordRepository recordRepository;
    UserRepository userRepository;
    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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

        Log.info("page " + p + " limit " + l);
        int page = Integer.parseInt(Optional.of(p).orElse("0"));
        int limit = Integer.parseInt(Optional.of(l).orElse("10"));

        long totalRecords = recordRepository.count();
        List<RecordItem> output = recordRepository.findAll().page(Page.of(page, limit)).list().stream().map(record -> {
            String format = record.getCreatedAt().atZone(ZoneId.of("America/Sao_Paulo")).toLocalDate()
                    .format(formatter);
            return new RecordItem(record.getId(), record.getAmount(), record.getDescription(),
                    record.getRecordType().name(), format);
        }).toList();
        return Response.ok(new PageRecord(
                output, totalRecords
        )).build();
    }

    public record PageRecord(List<RecordItem> items, Long totalRecords) {}

    public record RecordItem(Long id, BigDecimal amount, String description, String recordType, String createdAt) {
    }

    public record CreateRecordRequest(@PositiveOrZero BigDecimal amount, @NotBlank String description,
            @NotNull RecordType recordType, @NotBlank String from) {
    }

}
