package dev.matheuscruz.presentation;

import dev.matheuscruz.domain.Record;
import dev.matheuscruz.infra.persistence.RecordRepository;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import java.util.List;

@RolesAllowed({"USER"})
@Path("/api/records")
public class RecordResource {

    RecordRepository recordRepository;

    public RecordResource(
            RecordRepository recordRepository
    ) {
        this.recordRepository = recordRepository;
    }

    @GET
    public Response getRecords() {
        List<Record> output = recordRepository.findAll().list();
        return Response.ok(output).build();
    }

}
