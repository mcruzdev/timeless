package dev.matheuscruz.presentation;

import dev.matheuscruz.domain.Record;
import dev.matheuscruz.infra.persistence.RecordRepository;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RolesAllowed({"USER"})
@Path("/api/records")
public class RecordResource {

    RecordRepository recordRepository;
    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");


    public RecordResource(
            RecordRepository recordRepository
    ) {
        this.recordRepository = recordRepository;
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

}
