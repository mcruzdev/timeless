package dev.matheuscruz.presentation;

import dev.matheuscruz.domain.*;
import dev.matheuscruz.domain.Record;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
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

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@PathParam("id") Long id) {
        recordRepository.deleteById(id);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @POST
    public Response createRecord(@Valid CreateRecordRequest req) {

        User user = this.userRepository.findByPhoneNumber(req.from()).orElseThrow(ForbiddenException::new);
        Record record = new Record.Builder().userId(user.getId()).amount(req.amount()).description(req.description())
                .transaction(req.transaction()).category(req.category()).build();

        QuarkusTransaction.requiringNew().run(() -> this.recordRepository.persist(record));

        return Response.created(URI.create("/api/records/" + record.getId())).build();
    }

    @GET
    public Response getRecords(@RestQuery("page") String p, @RestQuery("limit") String l) {

        int page = Integer.parseInt(Optional.of(p).orElse("0"));
        int limit = Integer.parseInt(Optional.of(l).orElse("10"));

        long totalRecords = recordRepository.count();

        List<RecordItemResponse> output = recordRepository.findAll().page(Page.of(page, limit)).list().stream()
                .map(record -> {
                    String format = record.getCreatedAt().atZone(ZoneId.of("America/Sao_Paulo")).toLocalDate()
                            .format(formatter);
                    return new RecordItemResponse(record.getId(), record.getAmount(), record.getDescription(),
                            record.getTransaction().name(), format, record.getCategory().name());
                }).toList();

        List<Record> list = recordRepository.find("createdAt >= :instant AND createdAt <= :now",
                Parameters.with("instant", INSTANT_2025).and("now", Instant.now())).list();

        Optional<BigDecimal> totalExpenses = list.stream()
                .filter(item -> item.getTransaction().equals(Transactions.OUT)).map(Record::getAmount)
                .reduce(BigDecimal::add);

        Optional<BigDecimal> totalIn = list.stream().filter(item -> item.getTransaction().equals(Transactions.IN))
                .map(Record::getAmount).reduce(BigDecimal::add);

        return Response.ok(new PagedRecord(output, totalRecords, totalExpenses.orElse(BigDecimal.ZERO),
                totalIn.orElse(BigDecimal.ZERO))).build();
    }

    public record PagedRecord(List<RecordItemResponse> items, Long totalRecords, BigDecimal totalExpenses,
            BigDecimal totalIn) {
    }

    public record RecordItemResponse(Long id, BigDecimal amount, String description, String transaction,
            String createdAt, String category) {
    }

    public record CreateRecordRequest(@PositiveOrZero BigDecimal amount, @NotBlank String description,
            @NotNull Transactions transaction, @NotBlank String from, @NotNull Categories category) {
    }

}
