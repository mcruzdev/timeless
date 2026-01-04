package dev.matheuscruz.presentation;

import dev.matheuscruz.domain.AmountAndTypeOnly;
import dev.matheuscruz.domain.Record;
import dev.matheuscruz.domain.RecordRepository;
import dev.matheuscruz.domain.Transactions;
import dev.matheuscruz.domain.User;
import dev.matheuscruz.domain.UserRepository;
import dev.matheuscruz.presentation.data.CreateRecordRequest;
import dev.matheuscruz.presentation.data.PageRecord;
import dev.matheuscruz.presentation.data.RecordItemResponse;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import io.quarkus.security.Authenticated;
import jakarta.enterprise.context.RequestScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import dev.matheuscruz.presentation.data.UpdateRecordRequest;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.net.URI;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.Claims;
import org.jboss.resteasy.reactive.RestQuery;

@RequestScoped
@Path("/api/records")
@Authenticated
public class RecordResource {

    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Claim(standard = Claims.upn)
    String upn;

    RecordRepository recordRepository;
    UserRepository userRepository;

    public RecordResource(RecordRepository recordRepository, UserRepository userRepository) {
        this.recordRepository = recordRepository;
        this.userRepository = userRepository;
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        QuarkusTransaction.requiringNew().run(() -> recordRepository.delete("id = :id AND userId = :userId",
                Parameters.with("id", id).and("userId", upn)));
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Long id, @Valid UpdateRecordRequest req) {
        Record record = this.recordRepository.findById(id);

        if (record == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        if (!record.getUserId().equals(upn)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        
        QuarkusTransaction.requiringNew().run(() -> record.update(req));
        
        return Response.noContent().build();
    }

    @POST
    public Response createRecord(@Valid CreateRecordRequest req) {

        User user = this.userRepository.findByPhoneNumber(req.from()).orElseThrow(ForbiddenException::new);
        Record record = new Record.Builder().userId(user.getId()).amount(req.amount()).description(req.description())
                .transaction(req.transaction()).category(req.category()).build();

        if (!user.getId().equals(upn)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        QuarkusTransaction.requiringNew().run(() -> this.recordRepository.persist(record));
        return Response.created(URI.create("/api/records/" + record.getId())).build();
    }

    @GET
    public Response getRecords(@RestQuery("page") String p, @RestQuery("limit") String l) {

        int page = Integer.parseInt(Optional.of(p).orElse("0"));
        int limit = Integer.parseInt(Optional.of(l).orElse("10"));

        // TODO: https://github.com/mcruzdev/timeless/issues/125
        long totalRecords = recordRepository.count("userId = :userId", Parameters.with("userId", upn));

        // pagination
        List<RecordItemResponse> output = recordRepository.find("userId = :userId", Parameters.with("userId", upn))
                .page(Page.of(page, limit)).list().stream().map(record -> {
                    String format = record.getCreatedAt().atZone(ZoneId.of("America/Sao_Paulo")).toLocalDate()
                            .format(formatter);
                    return new RecordItemResponse(record.getId(), record.getAmount(), record.getDescription(),
                            record.getTransaction().name(), format, record.getCategory().name());
                }).toList();

        // calculate total expenses and total in
        List<AmountAndTypeOnly> amountAndType = recordRepository.getRecordsWithAmountAndTypeOnlyByUser(upn);
        Optional<BigDecimal> totalExpenses = amountAndType.stream()
                .filter(item -> item.getTransaction().equals(Transactions.OUT)).map(AmountAndTypeOnly::getAmount)
                .reduce(BigDecimal::add);

        Optional<BigDecimal> totalIn = amountAndType.stream()
                .filter(item -> item.getTransaction().equals(Transactions.IN)).map(AmountAndTypeOnly::getAmount)
                .reduce(BigDecimal::add);

        return Response.ok(new PageRecord(output, totalRecords, totalExpenses.orElse(BigDecimal.ZERO),
                totalIn.orElse(BigDecimal.ZERO))).build();
    }
}
